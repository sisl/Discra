package spark.driver

import net.liftweb.json._
import net.liftweb.json.Serialization.write

import breeze.linalg.DenseMatrix

import kafka.serializer.StringDecoder

import org.apache.log4j.{Level, Logger}

import org.apache.spark.SparkConf
import org.apache.spark.streaming._
import org.apache.spark.streaming.kafka._

import kafkapool._

import spark.worker.dronestate._
import spark.worker.grid._
import spark.worker.policy._

/**
 * Consumes messages from one or more topics in Kafka and resolves conflict.
 *
 * Usage: Streamer <brokers> <conflicts> <debug>
 *   <brokers> is a list of Kafka brokers; e.g., "localhost:9092"
 *   <conflicts> are Kafka topics to consume conflict messages from; e.g., "conflict"
 *   <debug> is a flag for debug mode (verbosity); e.g., "false"
 *
 *  Example:
 *    `$ Streamer broker1-host:port,broker2-host:port topic1,topic2 false`
 *
 *  Server startup:
 *    Kafka uses ZooKeeper so start a ZooKeeper server if you don't already
 *    have one. Use the convenience script packaged with kafka to get a quick-
 *    and-dirty single-node ZooKeeper instance.
 *
 *    From the Kafka root, run
 *      `$ bin/zookeeper-server-start.sh config/zookeeper.properties`.
 *
 *    Now start the Kafka server by running
 *      `$ bin/kafka-server-start.sh config/server.properties`.
 *
 *    If the topic to consume from has not been started, create a topic named
 *    "test" with a single partition and only one replica as follows.
 *      `$ bin/kafka-topics.sh --create --zookeeper localhost:2181 \
 *         --replication-factor 1 --partitions 1 --topic test`
 *
 *    Once these two servers have been started up and the topics created, you
 *    can start up Streamer. To verify that messages are being sent, you can
 *    start a command line consumer that will dump out messages to standard
 *    output as follows.
 *      `$ bin/kafka-console-consumer.sh --zookeeper localhost:2181 --topic <topic>`
 */
object Streamer {
  def main(args: Array[String]): Unit = {
    if (args.length < 3) {
      System.err.println(s"""
        |Usage: Streamer <brokers> <topics> <debug>
        |  <brokers> is a list of one or more Kafka brokers
        |  <topics> is a list of one or more kafka topics to consume from
        |  <debug> is a flag for debug mode (verbosity)
        |
        """.stripMargin)
      System.exit(1)
    }

    val Array(brokers, topics, debug) = args

    if (debug == "false") {
      Logger.getLogger("org").setLevel(Level.WARN)
    }

    // local StreamingContext with 2 working threads and batch interval of 1 second
    val conf = new SparkConf()
      .setAppName("UTMalpha")
      .setMaster("local[2]")
      .set("spark.executor.memory", "1g")
      .set("spark.rdd.compress","true")


    val ssc = new StreamingContext(conf, Seconds(1))
    val policy = ssc.sparkContext.broadcast(getPolicy())
    val producerPool = ssc.sparkContext.broadcast(KafkaPool(brokers))

    ssc.checkpoint("ckpt-policy")

    // direct Kafka stream with brokers and topics
    val topicsSet = topics.split(",").toSet
    val kafkaParams = Map[String, String]("metadata.broker.list" -> brokers)

    val rawConflicts = KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](
        ssc, kafkaParams, topicsSet)

    // whitespace-separated set of conflicts, which are in turn comma-separated DroneGlobalStates
    val advisories = rawConflicts
      .flatMap(_._2.split(" "))  // separate into conflict strings
      .map(rawDrones => getDrones(rawDrones.split(",")))  // build drone objects for policy
      .map { conflict =>  // generate advisories from policy
        val ids = conflict.map(_._1)
        val drones = conflict.map(_._2)
        val advs = policy.value.searchPolicy(drones)._1
        (ids, drones, advs).zipped.toArray
      }

    ssc.checkpoint("ckpt-advisory")

    advisories.map(alertDrones(_, producerPool.value)).print(0)  // need output to run ssc

    ssc.start()
    ssc.awaitTermination()
  }

  /** Returns the policy object that resovles conflict for each executor node. */
  private def getPolicy(filename: String = Const.UtilityFile): Policy = {
    val utility = Policy.readUtility(filename) match {
      case Some(rawUtility) =>
        if (rawUtility.cols == Const.UtilityCols && rawUtility.rows == Const.UtilityRows) {
          println("INFO utility read successfully")
        } else {
          println("WARN utility file might have been updated or corrupted")
        }
        rawUtility

      case None =>
        println("WARN utility read unsuccessful, returning empty DenseMatrix")
        new DenseMatrix[Double](0, 0)
    }
    Policy(utility, Const.ActionSet, Grid(Const.S1, Const.S2, Const.S3, Const.S4, Const.S5))
  }

  /** Returns ID-DroneGlobalState pairs. */
  private def getDrones(rawDrones: Array[String]): Array[(String, DroneGlobalState)] =
    rawDrones.map(unpackDrone)

  /** Unpacks string; e.g., rawDrone = "1337%120.03%32.01%123.0%10.3". */
  private def unpackDrone(rawDrone: String): (String, DroneGlobalState) = {
    val Array(id, latitude, longitude, heading, speed) = rawDrone.split("%")
    val drone =
      DroneGlobalState(
        latitude.toDouble,
        longitude.toDouble,
        heading.toDouble,
        speed.toDouble)

    (id, drone)
  }

  case class Advisories(advisories: List[Advisory])
  case class Advisory(gufi: String, clearOfConflict: String, waypoints: List[Waypoint])
  case class Waypoint(
      lat: String,      // m
      lon: String,      // m
      speed: String,    // m/s
      heading: String,  // rad
      period: String,   // s
      turn: String)     // rad/s

  /** Alerts drones through UTM client server and return json string. */
  private def alertDrones(
      conflict: Array[(String, DroneGlobalState, Double)],
      producer: KafkaPool): String = {

    val advisory = Advisories {
      for (idrone <- conflict.indices.toList) yield {
        val state = conflict(idrone)._2
        val bankAngle = conflict(idrone)._3

        val clearOfConflict = bankAngle match {
          case Const.ClearOfConflict => "true"
          case _ => "false"
        }

        val turnRate = bankAngle2turnRate(bankAngle, state.speed)

        Advisory(
          gufi = conflict(idrone)._1,
          clearOfConflict = clearOfConflict,
          waypoints = List(
            Waypoint(
              state.latitude.toString,
              state.longitude.toString,
              state.speed.toString,
              state.heading.toString,
              Const.DecisionPeriod.toString,
              turnRate.toString)))
      }
    }

    // create json string from the Advisory
    implicit val formats = DefaultFormats
    val json = write(advisory)

    // alert drone through UTM client server and return json string
    dummyAlertUTM(json, producer)
    json
  }

  /** Returns turn rate from bank angle in rad/s. */
  private def bankAngle2turnRate(bankAngle: Double, speed: Double): Double = {
    bankAngle match {
      case Const.ClearOfConflict => 0.0
      case _ => Const.G * math.tan(bankAngle) / speed
    }
  }

  private def meter2feet(meter: Double) = Const.Meter2Feet * meter

  // note: to be replaced once UTM client server API for sending advisories is available
  private def dummyAlertUTM(json: String, p: KafkaPool): Unit = p.send("advisory", json)
}
