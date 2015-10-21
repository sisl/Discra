package simulator

import java.text.DecimalFormat

import scala.collection.mutable

import kafka.serializer.StringDecoder

import net.liftweb.json.DefaultFormats
import net.liftweb.json.Serialization._

import org.apache.log4j.{Level, Logger}

import org.apache.spark.SparkConf
import org.apache.spark.streaming._
import org.apache.spark.streaming.dstream.InputDStream
import org.apache.spark.streaming.kafka._

import drone._
import kafkapool._

/**
 * Produces status update messages in the form of json strings as stipulated
 * by the UTM client server interface document:
 *   {"flightId":"1",
 *    "lat":-63.060318,
 *    "lon":56.432045,
 *    "alt":234.0,
 *    "speed":45,
 *    "heading":100}.
 * Note that we currently use SI units for convenience.
 *
 * Usage: Simulator <brokers> <debug>
 *   <brokers> is a list of Kafka brokers; e.g., "localhost:9092"
 *   <debug> is a flag for debug mode (verbosity); e.g., "false"
 *
 *  Example:
 *    `$ Simulator broker1-host:port,broker2-host:port topic1,topic2 false`
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
 *      `$ bin/kafka-console-consumer.sh --zookeeper <broker> --topic <topic>`
 */
object Simulator {
  def main(args: Array[String]): Unit = {
    if (args.length < 2) {
      System.err.println(s"""
        |Usage: Simulator <brokers> <debug>
        |<brokers> is a list of Kafka brokers; e.g., "localhost:9092"
        |<debug> is a flag for debug mode (verbosity); e.g., "false"
        |
        """.stripMargin)
      System.exit(1)
    }

    val Array(brokers, debug) = args

    if (debug == "false") {
      Logger.getLogger("org").setLevel(Level.WARN)
    }

    // local StreamingContext with 2 working threads and batch interval of 1 second
    val conf = new SparkConf()
      .setAppName("DroneSimulator")
      .setMaster("local[2]")
      .set("spark.executor.memory", "1g")
      .set("spark.rdd.compress","true")

    val ssc = new StreamingContext(conf, Seconds(1))
    val producerPool = ssc.sparkContext.broadcast(KafkaPool(brokers))

    val rawDrones: mutable.HashMap[String, Drone] = Const.DummyDrones
    val drones = ssc.sparkContext.broadcast(rawDrones)

    // initialize process by sending status
    drones.value.values.foreach { drone =>
      val status = jsonStatus(drone)
      producerPool.value.send("status", status)
    }

    // direct Kafka stream with brokers and topics for advisories
    val topicsSet = "advisory".split(",").toSet
    val kafkaParams = Map[String, String]("metadata.broker.list" -> brokers)

    val advisoryStream: InputDStream[(String, String)] =
      KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](
        ssc, kafkaParams, topicsSet)

    // execute advisories if one exists and simulate drone flight
    advisoryStream.map { jsonAdvisory =>
      val advisories = DroneAdvisory.getDroneAdvisories(jsonAdvisory._2)
      advisories.foreach{ advisory =>
        val drone = drones.value(advisory.gufi)
        drone.advisory = Some(advisory)
        drone.nextState(Const.StatusUpdatePeriod)
        producerPool.value.send("status", jsonStatus(drone))
      }
    }.print(0)  // need output to run ssc

    ssc.checkpoint("ckpt-advisory")

    ssc.start()
    ssc.awaitTermination()
  }

  case class Status(
      flightId: String,
      lat: String,      // in m
      lon: String,      // in m
      speed: String,    // in m/s
      heading: String)  // in rad

  val formatter = new DecimalFormat("#.###")

  /** Formats drone state into JSON string. */
  def jsonStatus(drone: Drone): String = {
    val status = Status(
      flightId = drone.gufi,
      lat = formatter.format(drone.latitude),
      lon = formatter.format(drone.longitude),
      speed = formatter.format(drone.speed),
      heading = formatter.format(drone.heading))

    implicit val formats = DefaultFormats
    write(status)
  }

}
