package tr.edu.ege.configuration

import com.typesafe.config.ConfigFactory

import scala.collection.JavaConverters._

object Configuration {
  private val config = ConfigFactory.load("kafka.conf")
    .withFallback(ConfigFactory.load("consumer.conf"))

  lazy val KAFKA_HOST: String = config.getString("kafka.host")
  lazy val KAFKA_PORT: String = config.getString("kafka.port")

  lazy val CONSUMER_URL: String = config.getString("consumer.url")
  lazy val CONSUMER_QUERY: List[String] = config.getStringList("consumer.query").asScala.toList
}
