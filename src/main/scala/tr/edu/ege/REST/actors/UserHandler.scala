package tr.edu.ege.REST.actors

import akka.actor.{Actor, ActorLogging}
import akka.event.LoggingReceive
import akka.pattern.ask
import akka.util.Timeout
import tr.edu.ege.actors.db.RedisDbT._
import tr.edu.ege.messages.PubHandler.{StartConsume, StartConsumeWithWait, StopConsume}
import tr.edu.ege.messages.UserHandler._
import tr.edu.ege.models.Resource
import io.circe.generic.auto._
import io.circe.syntax._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success}

class UserHandler extends Actor with ActorLogging {

  private val redisActor = context.actorSelection("/user/app/redisActor")
  implicit val timeout: Timeout = FiniteDuration(5, "seconds")

  override def receive: Receive = LoggingReceive {
    case GetUser(username) =>
      val currentSender = sender()
      log.info(s"Sender:${currentSender.toString()}")

      (redisActor ? FetchRequest(s"user:$username")).onComplete {
        case Success(value) =>
          value match {
            case FetchResult(result) =>
              result match {
                case Some(password: String) =>
                  log.debug(s"User is Found $username $password")
                  currentSender ! UserResponse(username, password)
                case None =>
                  log.debug(s"User:$username couldn't found at db.")
                  currentSender ! UserNotFound
              }
          }
        case Failure(exception) => throw exception
      }

    case AddUser(username, password) =>
      val currentSender = sender()
      (redisActor ? AddRequest(s"user:$username", password)).onComplete {
        case Success(value) => value match {
          case AddResult(res) if !res =>
            log.error("An error occurred while adding new user to Redis.")
            currentSender ! UserNotCreated
          case AddResult(res) if res =>
            log.debug("New User added to the Redis successfully.")
            currentSender ! UserCreated
        }
        case Failure(exception) => throw exception
      }

    case RegisterQuery(username, resource) =>
      (redisActor ? AddInSetRequest(s"user:${username}:topics", resource.url)).onComplete {
        case Success(value) => value match {
          case AddResult(status) if status =>
            log.debug("Topic successfully added to User Set.")
          case AddResult(status) if !status => log.error("Topic can not added to User Set.")
        }
        case Failure(exception) => throw exception
      }

    case DeregisterQuery(username, resource) =>
      (redisActor ? RemoveFromSetRequest(s"user:${username}:topics", resource.url)).onComplete {
        case Success(value) => value match {
          case AddResult(status) if status =>
            log.debug("Topic successfully removed from User Set.")
          case AddResult(status) if !status => log.error("Topic can not removed from User Set.")
        }
        case Failure(exception) => throw exception
      }

    case StartListen(username) =>
      val pubHandler = context.system.actorSelection(s"/user/webserver/pubhandler")
      (redisActor ? GetSetRequest(s"user:${username}:topics")).onComplete {
        case Success(value) => value match {
          case GetSetResult(topicUrls) =>
            topicUrls.foreach(topicUrl => {
              pubHandler ! StartConsumeWithWait(Resource(topicUrl, mayBeQuery = Some(List("IdList", "Id"))), User(username, ""))
            })
        }
        case Failure(exception) => throw exception
      }

    case StopListen(username) =>
      val pubHandler = context.system.actorSelection(s"/user/webserver/pubhandler")
      (redisActor ? GetSetRequest(s"user:${username}:topics")).onComplete {
        case Success(value) => value match {
          case GetSetResult(topics) =>
            topics.foreach(topic => {
              pubHandler ! StopConsume(Resource(topic, mayBeQuery = Some(List("IdList", "Id"))), User(username, ""))
            })
        }
        case Failure(exception) => throw exception

      }

    case GetQueryList(username) =>
      val currentSender = sender()
      (redisActor ? GetSetRequest(s"user:${username}:topics")).onComplete {
        case Success(value) => value match {
          case GetSetResult(topics) =>
            log.debug(s"Set is found for $username and topics are $topics")
            currentSender ! topics
        }
        case Failure(exception) => throw exception
      }
  }
}
