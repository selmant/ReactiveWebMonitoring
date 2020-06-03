package tr.edu.ege.REST.actorAPI

import akka.actor.ActorRef
import akka.util.Timeout
import akka.pattern.ask
import tr.edu.ege.messages.PubHandler.{ChangeResults, CheckForUpdates, StartConsume, StartConsumeWithWait, StopConsume}
import tr.edu.ege.messages.UserHandler.{GetUser, User, UserLoginResponse}
import tr.edu.ege.models.Resource

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

class PubHandlerApi(pubHandler: ActorRef) {
  implicit val timeout: Timeout = FiniteDuration(5, "seconds")

  def startConsume(resource: Resource, user: User): Unit = pubHandler ! StartConsume(resource, user)

  def startConsumeWithWait(resource: Resource, user: User): Unit = pubHandler ! StartConsumeWithWait(resource, user)

  def stopConsume(resource: Resource, user: User): Unit = pubHandler ! StopConsume(resource, user)

  def checkForUpdates(user: User): Future[ChangeResults] = (pubHandler ? CheckForUpdates(user)).mapTo[ChangeResults]

}
