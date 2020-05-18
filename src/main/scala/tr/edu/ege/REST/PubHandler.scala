package tr.edu.ege.REST

import akka.actor.{Actor, ActorLogging}
import akka.event.LoggingReceive
import akka.pattern.ask
import akka.util.Timeout
import tr.edu.ege.actors.db.RedisDbT._
import tr.edu.ege.messages.Messages.Submit
import tr.edu.ege.messages.PubHandler.SubscribePub
import tr.edu.ege.messages.UserHandler._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success}

class PubHandler extends Actor with ActorLogging {

  private val controllerActor = context.actorSelection("/user/app/controller")
  implicit val timeout: Timeout = FiniteDuration(5, "seconds")

  override def receive: Receive = LoggingReceive {
    case SubscribePub(resource, user) =>
      controllerActor ! Submit(resource, Some(user))
      log.debug("Request handled")
  }
}