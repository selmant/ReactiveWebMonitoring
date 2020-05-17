package tr.edu.ege.REST

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import tr.edu.ege.messages.UserHandler.{AddUser, DeleteUser, GetUser, UpdateUser, User, UserResponse}

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

class UserHandlerApi(userHandler: ActorRef) {

    implicit val timeout: Timeout = FiniteDuration(5, "seconds")

    def addUser(username: String, password: String): Future[UserResponse] = (userHandler ? AddUser(username,password)).mapTo[UserResponse]
    def getUser(username: String): Future[Option[User]] = (userHandler ? GetUser(username)).mapTo[Option[User]]
    def deleteUser(username: String): Future[Option[User]] = (userHandler ? DeleteUser(username)).mapTo[Option[User]]
    def updateUser(username: String): Future[Option[User]] = (userHandler ? UpdateUser(username)).mapTo[Option[User]]
}
