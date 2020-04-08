package tr.edu.ege.actors

import akka.actor.{Actor, ActorLogging, Props}
import org.apache.kafka.clients.consumer.ConsumerRecord
import tr.edu.ege.messages.Messages.ExtractXMLResult
import tr.edu.ege.models.Resource

object ResourceConsumer {

  case object Ack

  case object StreamInitialized

  case object StreamCompleted

  final case class StreamFailure(ex: Throwable)

}

class ResourceConsumer(resource: Resource) extends Actor with ActorLogging {

  import ResourceConsumer._

  override def preStart(): Unit = log.info(s"Consumer Sink Actor started. Resource: ${resource.toString}")

  override def postStop(): Unit = log.info(s"Consumer Sink Actor stopped.")

  override def receive: Receive = {
    case StreamInitialized =>
      log.info("Stream initialized!")
      sender() ! Ack // ack to allow the stream to proceed sending more elements

    case record: ConsumerRecord[String, String] =>
      log.info(s"Consumed a new stream message for url:${resource.url}")
      log.info(s"Message: $record")
      if (resource.mayBeQuery.isDefined) {
        val extractor = context.actorOf(Props[XmlExtractor])
        extractor ! ExtractXMLResult(resource, record.value())
      }
      sender() ! Ack // ack to allow the stream to proceed sending more elements

    case StreamCompleted =>
      log.info("Stream completed!")

    case StreamFailure(ex) =>
      log.error(ex, "Stream failed!")

    case default =>
      log.warning(s"Unknown message received.")
      log.info(s"Message: $default")

  }
}