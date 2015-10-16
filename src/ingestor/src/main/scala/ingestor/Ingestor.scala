package ingestor

import net.liftweb.json._

import kafka.serializer.StringDecoder

import org.apache.log4j.{Level, Logger}

import org.apache.spark.streaming.dstream.InputDStream
import org.apache.spark.streaming.kafka.KafkaUtils
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.SparkConf

import kafkapool._

/**
 * Produces messages for a single topic in Kafka by ingesting an input
 * stream of UTM client server publication service.
 *
 * Usage: Ingestor <brokers> <conflict> <debug>
 *    <brokers> is a list of one or more Kafka brokers; e.g., "localhost:9092"
 *    <conflict> is a Kafka topic to produce conflict messages for; e.g., "conflict"
 *    <debug> is a flag for debug mode (verbosity); e.g., "false"
 *
 *  Example:
 *    `$ Ingestor broker1-host:port,broker2-host:port topic false`
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
 *    Once these two servers have been started up, you can start Ingestor. To
 *    verify that messages are being sent, you can start a command line
 *    consumer that will dump out messages to standard output as follows.
 *      `$ bin/kafka-console-consumer.sh --zookeeper <broker> --topic <topic>`
 */
object Ingestor {
  def main(args: Array[String]): Unit = {
    if (args.length < 3) {
      System.err.println(s"""
        |Usage: Ingestor <brokers> <conflict> <debug>
        |  <brokers> is a list of one or more Kafka brokers
        |  <conflict> is a Kafka topic to produce formatted conflict messages for
        |  <debug> is a flag for debug mode (verbosity)
        |
        """.stripMargin)
      System.exit(1)
    }

    val Array(brokers, conflict, debug) = args

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
    simProducer(conflict, producerPool.value, ssc)
  }

  case class Status(
      flightId: String,
      lat: String,      // in m
      lon: String,      // in m
      speed: String,    // in m/s
      heading: String)  // in rad

  /** Ingests simulator app messages from Kafka to produce conflict messages. */
  def simProducer(
      conflict: String,
      producer: KafkaPool,
      ssc: StreamingContext): Unit = {

    // direct Kafka stream with brokers and topics
    val topicsSet = "status".split(",").toSet
    val kafkaParams = Map[String, String]("metadata.broker.list" -> "localhost:9092")

    val statusStream: InputDStream[(String, String)] =
      KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](
        ssc, kafkaParams, topicsSet)

    // map-reduce statuses in <statusStream> into conflict json string; note assumption
    // that every rdd in the dstream contains all relevant aircraft without cutoff
    val statusStrings = statusStream.map { status =>
      implicit val formats = DefaultFormats
      getDroneString(parse(status._2).extract[Status])
    }

    ssc.checkpoint("ckpt-status")

    val conflicts = statusStrings.reduce(_ + _)
    conflicts.map(producer.send(conflict, _)).print(0)  // need output to run ssc

    ssc.start()
    ssc.awaitTermination()
  }

  /** Formats drone flight state into string. */
  def getDroneString(status: Status): String = {
    status.flightId + "%" +
    status.lat + "%" +
    status.lon + "%" +
    status.heading + "%" +
    status.speed + ","
  }
}
