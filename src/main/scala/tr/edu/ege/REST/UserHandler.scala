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
                        case Some(user: String) =>
                            log.debug(s"User is Found $user")
                            currentSender ! user
                        case None =>
                            val currentSender = sender()
                            currentSender ! UserNotFound
                    }
            }
        case AddUser(username, user) =>
            (redisActor ? AddRequest(s"user:$username", user)).onComplete {
                case Success(value) => value match {
                    case AddResult(res) if !res =>
                        log.error("An error occurred while adding new user to Redis.")
                    case AddResult(res) if res =>
                        log.debug("New User added to the Redis successfully.")
                }
                case Failure(exception) => throw exception
            }
    }
}
