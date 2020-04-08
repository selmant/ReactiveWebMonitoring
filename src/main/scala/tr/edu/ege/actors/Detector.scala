package tr.edu.ege.actors

import akka.actor.{Actor, ActorLogging}
import akka.event.LoggingReceive
import akka.pattern.ask
import akka.util.Timeout
import io.circe.generic.auto._
import io.circe.syntax._
import tr.edu.ege.actors.db.RedisDbT.{AddRequest, AddResult, FetchRequest, FetchResult}
import tr.edu.ege.messages.Messages
import tr.edu.ege.messages.Messages.UniqueResult

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success}

class Detector extends Actor with ActorLogging {
  implicit val timeout: Timeout = FiniteDuration(5, "seconds")
  private val redisActor = context.actorSelection("/user/app/redisActor")

  override def receive: Receive = LoggingReceive {
    case result: Messages.Result =>
      (redisActor ? FetchRequest(result.resource.asJson.noSpaces)).onComplete {
        case Success(value) =>
          value match {
            case FetchResult(res) => res match {
              case Some(payload) =>
                if (payload.equals(result.payload)) {
                  log.info("### Result is the same as before.")
                } else {
                  handleUniqueResult(result)
                }

              case None =>
                handleUniqueResult(result)
            }
          }

        case Failure(exception) => throw exception
      }
  }

  private def handleUniqueResult(result: Messages.Result): Unit = {
    log.info("### Found unique result!")

    (redisActor ? AddRequest(result.resource.asJson.noSpaces, result.payload)).onComplete {
      case Success(value) => value match {
        case AddResult(res) if !res =>
          log.error("An error occurred while adding element to Redis.")

        case AddResult(res) if res =>
          log.debug("Unique result added to the Redis successfully.")
          val uniqueResult = UniqueResult.fromResult(result)
          //    context.parent ! uniqueResult
          val publisher = context.actorSelection("/user/app/publisher")
          publisher ! uniqueResult

      }
      case Failure(exception) => throw exception
    }
  }
}
