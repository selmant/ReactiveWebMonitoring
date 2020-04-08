package tr.edu.ege.actors

import java.util.concurrent.Executor

import akka.actor.{Actor, ActorLogging, Status}
import akka.event.LoggingReceive
import akka.pattern.pipe
import tr.edu.ege.client.{AsyncWebClient, WebClient}
import tr.edu.ege.messages.Messages
import tr.edu.ege.models.Resource
import tr.edu.ege.client.{AsyncWebClient, WebClient}
import tr.edu.ege.messages.Messages
import tr.edu.ege.models.Resource

import scala.concurrent.ExecutionContext

class Getter(resource: Resource) extends Actor with ActorLogging {
  implicit val executor: Executor with ExecutionContext = context.dispatcher.asInstanceOf[Executor with ExecutionContext]

  private def webClient: WebClient = AsyncWebClient

  webClient get resource.url pipeTo self

  def receive: Receive = LoggingReceive {
    case body: String =>
      log.debug("Body fetched successfully.")
      log.debug("Result sending to the parent actor.")
      context.parent ! Messages.Result(resource, body)
      context.stop(self)

    case _: Status.Failure =>
      log.debug("Got status failure, stopping self!")
      context.stop(self)
  }
}
