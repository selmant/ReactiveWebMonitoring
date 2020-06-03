package tr.edu.ege

import java.util.UUID

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import tr.edu.ege.actors.ResourceConsumer
import tr.edu.ege.actors.ResourceConsumer._
import tr.edu.ege.actors.db.RedisDbService
import tr.edu.ege.actors.db.RedisDbT.{FetchRequest, FetchResult}
import tr.edu.ege.configuration.Configuration
import tr.edu.ege.models.Resource

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object DigestionKafka extends App with LazyLogging {

  implicit val _system: ActorSystem = ActorSystem("DigestionSystem")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val timeout: Timeout = FiniteDuration(5, "seconds")

  private val url: String = Configuration.CONSUMER_URL
  private val query: List[String] = Configuration.CONSUMER_QUERY

  logger.info(s"Kafka digestion system starting up with url:$url query:$query")

  val redisActor: ActorRef = _system.actorOf(Props(new RedisDbService("redis.conf", "scredis", FiniteDuration(5, "seconds"))), "redisActor")

  private val consumerActor = _system.actorOf(
    props = Props(new ResourceConsumer(Resource(url, mayBeQuery = Option.apply(query)))),
    name = "consumer-actor"
  )

  private val consumerSink = Sink.actorRefWithBackpressure(
    ref = consumerActor,
    onInitMessage = StreamInitialized,
    ackMessage = Ack,
    onCompleteMessage = StreamCompleted,
    onFailureMessage = StreamFailure
  )

  private val consumerSettings = ConsumerSettings(_system, new StringDeserializer, new StringDeserializer)
    .withBootstrapServers(s"${Configuration.KAFKA_HOST}:${Configuration.KAFKA_PORT}")
    .withGroupId(s"consumer-group-${UUID.randomUUID()}")
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")
  (redisActor ? FetchRequest(url)).onComplete {
    case Success(value) => value match {
      case FetchResult(maybeTopic) => maybeTopic match {
        case Some(topic) =>
          logger.info(s"Kafka consumer starting for topic: $topic")
          val runningStream = Consumer.plainSource(
            settings = consumerSettings.withClientId(s"client-${UUID.randomUUID()}"),
            subscription = Subscriptions.topics(topic)
          )
            .to(consumerSink)
            .run()

          sys.addShutdownHook {
            logger.info("System shutting down...")
            Await.ready(runningStream.shutdown, 2.seconds)
            Await.ready({
              materializer.shutdown
              _system.terminate
            }, 10.seconds)
          }

        case None => throw new IllegalStateException("Topic id not found at Redis")
      }
    }
    case Failure(exception) => throw exception
  }

  logger.info("System initialized.")
}