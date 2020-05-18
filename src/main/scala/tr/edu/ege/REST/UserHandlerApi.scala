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
    def getUser(username: String): Future[User] = (userHandler ? GetUser(username)).mapTo[User]
    def deleteUser(username: String): Future[User] = (userHandler ? DeleteUser(username)).mapTo[User]
    def updateUser(username: String): Future[User] = (userHandler ? UpdateUser(username)).mapTo[User]
}
