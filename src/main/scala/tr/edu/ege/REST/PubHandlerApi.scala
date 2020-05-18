package tr.edu.ege.REST

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import tr.edu.ege.messages.Messages.Submit
import tr.edu.ege.messages.PubHandler.SubscribePub
import tr.edu.ege.messages.UserHandler.{AddUser, DeleteUser, GetUser, UpdateUser, User, UserResponse}
import tr.edu.ege.models.Resource

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

class PubHandlerApi(pubHandler: ActorRef) {
  implicit val timeout: Timeout = FiniteDuration(5, "seconds")

  def subscribePub(resource: Resource, user: User): Unit ={
    pubHandler ! SubscribePub(resource, user)
  }

}
