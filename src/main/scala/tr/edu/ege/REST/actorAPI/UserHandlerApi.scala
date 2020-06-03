package tr.edu.ege.REST.actorAPI

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import tr.edu.ege.messages.UserHandler.{AddUser, DeregisterQuery, GetQueryList, GetUser, RegisterQuery, StartListen, UserLoginResponse, UserRegisterResponse}
import tr.edu.ege.models.Resource

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

class UserHandlerApi(userHandler: ActorRef) {

  implicit val timeout: Timeout = FiniteDuration(5, "seconds")

  def addUser(username: String, password: String): Future[UserRegisterResponse] = (userHandler ? AddUser(username, password)).mapTo[UserRegisterResponse]

  def getUser(username: String): Future[UserLoginResponse] = (userHandler ? GetUser(username)).mapTo[UserLoginResponse]

  def registerQuery(username: String, resource: Resource): Unit = userHandler ! RegisterQuery(username, resource)

  def deregisterQuery(username: String, resource: Resource): Unit = userHandler ! DeregisterQuery(username, resource)

  def getQueryList(username: String): Future[Set[String]] = (userHandler ? GetQueryList(username)).mapTo[Set[String]]

  def startListen(username: String): Unit = userHandler ! StartListen(username)

}
