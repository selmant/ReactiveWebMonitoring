package tr.edu.ege.actors

import java.util.{Properties, UUID}

import akka.actor.{Actor, ActorLogging}
import akka.event.LoggingReceive
import akka.pattern.ask
import akka.util.Timeout
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig, ProducerRecord}
import tr.edu.ege.actors.db.RedisDbT.{AddRequest, AddResult, FetchRequest, FetchResult}
import tr.edu.ege.configuration.Configuration
import tr.edu.ege.messages.Messages.UniqueResult
import tr.edu.ege.models.Resource
import tr.edu.ege.models.Resource

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success}

object Publisher {
  val server = s"${Configuration.KAFKA_HOST}:${Configuration.KAFKA_PORT}"

  val producerProperties = new Properties()
  producerProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, server)
  producerProperties.put(
    ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
    "org.apache.kafka.common.serialization.StringSerializer"
  )
  producerProperties.put(
    ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
    "org.apache.kafka.common.serialization.StringSerializer"
  )
}

class Publisher extends Actor with ActorLogging {
  implicit val timeout: Timeout = FiniteDuration(5, "seconds")
  private val redisActor = context.actorSelection("/user/app/redisActor")

  val producer = new KafkaProducer[String, String](Publisher.producerProperties)

  override def receive: Receive = LoggingReceive {
    case UniqueResult(resource: Resource, payload: String) =>
      val url: String = resource.url
      val key: String = resource.mayBeQuery match {
        case Some(queryList) => queryList.mkString(" ")
        case None => "|root|"
      }
      val value: String = payload

      (redisActor ? FetchRequest(url)).onComplete {
        case Success(s) =>
          s match {
            case FetchResult(res) =>
              res match {
                case Some(topic) => publishRecord(key, value, topic)

                case None =>
                  val newTopic = UUID.randomUUID().toString
                  log.info(s"No topic value found for url:$key at Redis. New topic:$newTopic generated for url:$url")
                  (redisActor ? AddRequest(url, newTopic)).onComplete {
                    case Success(v) => v match {
                      case AddResult(res) if res => publishRecord(key, value, newTopic)

                      case AddResult(res) if !res => log.error("An error occurred while adding element to Redis.")
                    }
                    case Failure(exception) => throw exception
                  }
              }
          }

        case Failure(exception) => throw exception
      }
  }

  private def publishRecord(key: String, value: String, topic: String) = {
    log.info("New record publishing...\nTopic:{}, Key:{}, Value:{}", topic, key, value)

    val record = new ProducerRecord[String, String](topic, key, value)
    try {
      producer.send(record).get
    } catch {
      case e: Throwable => log.error(e, "An error occurred while sending record to Apache Kafka.")
    }
  }

  override def postStop(): Unit = {
    producer.close()
  }
}
