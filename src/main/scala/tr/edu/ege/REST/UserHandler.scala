package tr.edu.ege.REST

import akka.actor.{Actor, ActorLogging}
import akka.event.LoggingReceive
import akka.pattern.ask
import akka.util.Timeout
import tr.edu.ege.actors.db.RedisDbT._
import tr.edu.ege.messages.UserHandler._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success}

class UserHandler extends Actor with ActorLogging {

    private val redisActor = context.actorSelection("/user/app/redisActor")
    implicit val timeout: Timeout = FiniteDuration(5, "seconds")

    override def receive: Receive = LoggingReceive {
        case GetUser(username) =>
            val currentSender = sender()
            (redisActor ? FetchRequest(s"user:$username")).onComplete {
                case Success(value) =>
                    value match {
                        case FetchResult(result) =>
                            result match {
                                case Some(password: String) =>
                                    log.info(s"User is Found $username $password")
                                    currentSender ! User(username, password)
                                case None =>
                                    log.info(s"User is Not Found")
                                    currentSender ! User("", "")
                            }
                        case None => currentSender ! User("", "")
                    }
                case _ => currentSender ! User("", "")
            }
        case AddUser(username, password) =>
            val currentSender = sender()
            (redisActor ? AddRequest(s"user:$username", password)).onComplete {
                case Success(value) => value match {
                    case AddResult(res) if !res =>
                        log.error("An error occurred while adding new user to Redis.")
                        currentSender ! UserAlreadyExist
                    case AddResult(res) if res =>
                        log.info("New User added to the Redis successfully.")
                        currentSender ! UserCreated


                }
                case Failure(exception) => throw exception
            }
    }
}
