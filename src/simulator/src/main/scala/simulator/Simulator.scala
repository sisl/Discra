package simulator

import java.io._

import java.text.DecimalFormat

import java.util
import java.util.Date

import org.joda.time.{DateTimeZone, DateTime}

import scala.collection.mutable

import scala.io.Source

import scala.sys.process._

import kafka.serializer.StringDecoder

import net.liftweb.json.DefaultFormats
import net.liftweb.json.Serialization._

import org.apache.kafka.clients.producer.{ProducerConfig, KafkaProducer}

import org.apache.log4j.{Level, Logger}

import org.apache.spark.SparkConf
import org.apache.spark.streaming._
import org.apache.spark.streaming.dstream.InputDStream
import org.apache.spark.streaming.kafka._

import drone._
import kafkapool._

import scala.util.Random

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

    // submit, check, and activate plan (note: change times in .xml files)
    val drones: mutable.HashMap[String, Drone] = Const.DummyDrones/*submitUTM()
    checkUTM(drones)
    activateUTM(drones)*/

    // local StreamingContext with 2 working threads and batch interval of 1 second
    val conf = new SparkConf()
      .setAppName("DroneSimulator")
      .setMaster("local[2]")
      .set("spark.executor.memory", "1g")
      .set("spark.rdd.compress","true")

    val ssc = new StreamingContext(conf, Seconds(1))
    val producerPool = ssc.sparkContext.broadcast(KafkaPool(brokers))

    // initialize process by sending status

    drones.foreach { droneElem =>
      val status = jsonStatus(droneElem._2)
      producerPool.value.send("status", status)
    }

    // dummy listener to "advisory" topic (instead of client server)
    // direct Kafka stream with brokers and topics
    val topicsSet = "advisory".split(",").toSet
    val kafkaParams = Map[String, String]("metadata.broker.list" -> brokers)

    val advisoryStream: InputDStream[(String, String)] =
      KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](
        ssc, kafkaParams, topicsSet)

    advisoryStream.map { jsonAdvisories =>
      val advisories = DroneAdvisory.getDroneAdvisories(jsonAdvisories._2)

      advisories.foreach { droneAdvisory =>
        drones(droneAdvisory.gufi).nextState(droneAdvisory)
        val status = jsonStatus(drones(droneAdvisory.gufi))
        producerPool.value.send("status", status)
        updateUTM(drones(droneAdvisory.gufi))
      }

      // note: hack to simulate 5 seconds passing; assume parallel workers
      Thread.sleep(Const.StatusUpdatePeriod)
    }.print(0)  // need output to run ssc

    ssc.checkpoint("ckpt-advisory")

    ssc.start()
    ssc.awaitTermination()
  }

  def getProducer(advisory: String, brokers: String): KafkaProducer[String, String] = {
    // zookeeper connection properties
    val props = new util.HashMap[String, Object]()
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers)
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
      "org.apache.kafka.common.serialization.StringSerializer")
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
      "org.apache.kafka.common.serialization.StringSerializer")

    new KafkaProducer[String, String](props)
  }

  case class Status(
      flightId: String,
      lat: String,      // in m
      lon: String,      // in m
      speed: String,    // in m/s
      heading: String)  // in rad

  val formatter = new DecimalFormat("#.###")

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

  def submitUTM(): mutable.HashMap[String, Drone] = {
    val dronemap = new mutable.HashMap[String, Drone]()
    val drones = Array(Const.Drone0, Const.Drone1, Const.Drone2)

    val inname = "utmops/insert_operations.xml"
    val outname = "utmops/insert_operations_updated.xml"

    val rng = new Random()
    val nextInt = rng.nextInt(Const.RandLatLon)

    for (idrone <- 0 until Const.NDrones) {
      replacesubmit(idrone + nextInt, inname, outname)

      val cmd = Seq(
        "curl",
        "-v",
        "-u", "stanford:KarNeva1!",
        "-k",
        "-X", "POST",
        "-d", "@" + outname,
        "-H", "Content-type: application/xml",
        "https://tmiserver.arc.nasa.gov/geoserver/ows")

      val res = cmd.!!
      drones(idrone).gufi = getgufi(res, idrone)
      dronemap += drones(idrone).gufi -> drones(idrone)
    }

    Thread.sleep(Const.TenSeconds)  // for activation catchup
    println("successfully submitted plans for drones")
    dronemap.keys.foreach { key: String => println("\t" + key) }

    dronemap
  }

  def replacesubmit(idrone: Int, inname: String, outname: String): Unit = {
    val infile = Source.fromFile(inname)
    try {
      val outfile = new File(outname)
      val writer = new BufferedWriter(new FileWriter(outfile))

      infile.getLines().foreach { line =>
        if (line.contains("<utm:effective_time_begin>")) {
          val now = new Date()
          val utcString = new DateTime(now)
            .plus(Const.TenSeconds)
            .withZone(DateTimeZone.UTC).toString
          writer.write("<utm:effective_time_begin>" + utcString + "</utm:effective_time_begin>")
        } else {
          val CoordPattern = """(.*[0-9]+.[0-9]+),(.*[0-9]+.[0-9]+)""".r
          CoordPattern.findFirstIn(line) match {
            case Some(xml) => xml match {
              case CoordPattern(lat, lon) =>
                val latdd = lat.toDouble + idrone
                val londd = lon.toDouble + idrone
                writer.write("           " + latdd + "," + londd)
              case _ => writer.write(line)
            }
            case None => writer.write(line)
          }
        }
        writer.newLine()
      }

      writer.flush()
      writer.close()
    } catch {
      case ex: FileNotFoundException =>
        throw new FileNotFoundException("couldn't find " + inname)
      case ex: IOException =>
        throw new IOException("couldn't read " + inname)
    }
  }

  def getgufi(raw: String, idrone: Int): String = {

    println(raw)

    val GufiPattern = """.*fid--(.*)_(.*)_(.*)".*""".r
    GufiPattern.findFirstIn(raw) match {
      case Some(xml) =>
        xml match {
          case GufiPattern(a, b, c) => "fid--" + a + "_" + b + "_" + c
          case _ => throw new Error("could not find drone " + idrone + "'s gufi")
        }
      case None => throw new Error("could not find drone " + idrone + "'s gufi")
    }
  }

  def checkUTM(drones: mutable.HashMap[String, Drone]): Unit = {
    for (gufi <- drones.keys) {
      val cmd = Seq(
        "curl",
        "-v",
        "-u", "stanford:KarNeva1!",
        "-k",
        "-G",
        "https://tmiserver.arc.nasa.gov/geoserver/utm/ows?service=WFS" +
        "&version=1.1.0&request=GetFeature&typeName=utm:operations_all&CQL_FILTER=(gufi='" +
         gufi + "')")
      println("submission for " + gufi + " is " + getstate(cmd.!!, gufi))
    }
  }

  def getstate(raw: String, gufi: String): String = {
    val StatePattern = """.*<utm:state>(.)<.*""".r
    StatePattern.findFirstIn(raw) match {
      case Some(xml) =>
        xml match {
          case StatePattern(state) => state match {
            case "R" => "rejected"
            case "A" => "accepted"
            case _ => throw new Error("unknown utm state")
          }
          case _ => throw new Error("could not parse utm state")
        }
      case None => throw new Error("invalid server response; check xml for " + gufi)
    }
  }

  def activateUTM(drones: mutable.HashMap[String, Drone]): Unit = {
    val inname = "utmops/insert_operation_messages_all_clear.xml"
    val outname = "utmops/insert_operation_messages_all_clear_updated.xml"
    for (gufi <- drones.keys) {
      replacegufi(gufi, inname, outname)
      val cmd = Seq(
        "curl",
        "-v",
        "-u", "stanford:KarNeva1!",
        "-k",
        "-X", "POST",
        "-d", "@" + outname,
        "-H", "Content-type: application/xml",
        "https://tmiserver.arc.nasa.gov/geoserver/ows")
      checkActivate(cmd.!!, gufi)
    }
  }

  def replacegufi(gufi: String, inname: String, outname: String): Unit = {
    val infile = Source.fromFile(inname)
    try {
      val outfile = new File(outname)
      val writer = new BufferedWriter(new FileWriter(outfile))

      infile.getLines().foreach { line =>
        if (line.contains("<utm:gufi>")) {
          writer.write("      <utm:gufi>" + gufi + "</utm:gufi>")
        } else {
          writer.write(line)
        }
        writer.newLine()
      }

      writer.flush()
      writer.close()
    } catch {
      case ex: FileNotFoundException =>
        throw new FileNotFoundException("couldn't find " + inname)
      case ex: IOException =>
        throw new IOException("couldn't read " + inname)
    }
  }

  def checkActivate(raw: String, gufi: String): Unit = {
    val StatePattern = """.*<wfs:Status>.*<wfs:(.*)/>.*</wfs:Status>.*""".r
    StatePattern.findFirstIn(raw) match {
      case Some(xml) =>
        xml match {
          case StatePattern(status) => status match {
            case "SUCCESS" => println("successful activation")
            case "FAILED" => println("unsuccessful activation")
            case _ => throw new Error("unknown wfs status")
          }
          case _ => throw new Error("could not parse wfs status")
        }
      case None => throw new Error("could not find wfs status for " + gufi)
    }
  }

  /** Updates UTM using a dummy set of lat-long coordinates (dec deg). */
  def updateUTM(drone: Drone): Unit = {
    val inname = "utmops/insert_positions.xml"
    val outname = "utmops/insert_positions_updated.xml"
    replacegufi(drone.gufi, inname, outname)
    replacecoord(drone.latitude, drone.longitude, inname, outname)

    val cmd = Seq(
      "curl",
      "-v",
      "-u", "stanford:KarNeva1!",
      "-k",
      "-X", "POST",
      "-d", "@" + outname,
      "-H", "Content-type: application/xml",
      "https://tmiserver.arc.nasa.gov/geoserver/ows")
    checkUpdate(drone.gufi, cmd.!!)
  }

  def replacecoord(lat: Double, lon: Double, inname: String, outname: String): Unit = {
    val infile = Source.fromFile(inname)
    try {
      val outfile = new File(outname)
      val writer = new BufferedWriter(new FileWriter(outfile))

      infile.getLines().foreach { line =>
        val CoordPattern = """(.*[0-9]+.[0-9]+),(.*[0-9]+.[0-9]+)""".r
        CoordPattern.findFirstIn(line) match {
          case Some(xml) => xml match {
            case CoordPattern(latact, lonact) =>
              val latitude = getlatitude(lat + fromlatitude(latact.toDouble))
              val longitude = getlongitude(lon +
                fromlongitude(lonact.toDouble, math.cos(latitude)), latitude)
              writer.write("           " + latitude + "," + longitude)
            case _ => writer.write(line)
          }
          case None => writer.write(line)
        }
        writer.newLine()
      }

      writer.flush()
      writer.close()
    } catch {
      case ex: FileNotFoundException =>
        throw new FileNotFoundException("couldn't find " + inname)
      case ex: IOException =>
        throw new IOException("couldn't read " + inname)
    }
  }

  // quick and dirty estimate of latitude
  def getlatitude(lat: Double): Double =
    lat / Const.LatLonMagic

  // quick and dirty estimate of longitude
  def getlongitude(lon: Double, latact: Double): Double =
    lon / (Const.LatLonMagic * math.cos(latact))

  def fromlatitude(lat: Double): Double =
    lat * Const.LatLonMagic

  def fromlongitude(lon: Double, latact: Double): Double =
    lon * Const.LatLonMagic * math.cos(latact)

  def checkUpdate(gufi: String, raw: String): Unit = {
    val StatePattern = """.*<wfs:Status>.*<wfs:(.*)/>.*</wfs:Status>.*""".r
    StatePattern.findFirstIn(raw) match {
      case Some(xml) =>
        xml match {
          case StatePattern(status) => status match {
            case "SUCCESS" => println("successful update")
            case "FAILED" => println("unsuccessful update")
            case _ => throw new Error("unknown wfs update status")
          }
          case _ => throw new Error("could not parse wfs update status")
        }
      case None => throw new Error("could not find wfs update status for " + gufi)
    }
  }
}
