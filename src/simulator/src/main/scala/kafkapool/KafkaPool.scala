package kafkapool

import java.util

import org.apache.kafka.clients.producer.{ProducerConfig, ProducerRecord, KafkaProducer}

/** Creates a pool of Kafka producers that each executor takes turn using. */
class KafkaPool(createProducer: () => KafkaProducer[String, String])
  extends Serializable {

  lazy val producer = createProducer()
  def send(topic: String, value: String): Unit =
    producer.send(new ProducerRecord(topic, value))
}

object KafkaPool {
  def apply(brokers: String): KafkaPool = {

    val config = new util.HashMap[String, Object]()
    config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers)
    config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
      "org.apache.kafka.common.serialization.StringSerializer")
    config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
      "org.apache.kafka.common.serialization.StringSerializer")

    val f = () => {
      val producer = new KafkaProducer[String, String](config)

      sys.addShutdownHook {
        producer.close()
      }

      producer
    }
    new KafkaPool(f)
  }
}