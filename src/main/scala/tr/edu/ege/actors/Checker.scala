package tr.edu.ege.actors

import java.util.UUID

import akka.actor.{Actor, ActorLogging, OneForOneStrategy, Props, ReceiveTimeout, SupervisorStrategy, Terminated}
import akka.event.LoggingReceive
import akka.pattern.ask
import akka.util.Timeout
import tr.edu.ege.actors.db.RedisDbT.{AddRequest, AddResult, FetchRequest, FetchResult}
import tr.edu.ege.messages.Messages
import tr.edu.ege.models.Job

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Random, Success}

class Checker extends Actor with ActorLogging {
  implicit val timeout: Timeout = FiniteDuration(5, "seconds")

  private val redisActor = context.actorSelection("/user/app/redisActor")

  override val supervisorStrategy: OneForOneStrategy = OneForOneStrategy(maxNrOfRetries = 5) {
    case _: Exception => SupervisorStrategy.Restart
  }

  //  context.setReceiveTimeout(100.seconds)

  override def receive: Receive = LoggingReceive {
    case msg@Messages.Check(resource) =>
      log.info(s"Got check message:$msg")

      // TODO
      // Scheduler zamanlamaları farklı olan joblar için aynı resource'a
      // tekrardan scheduler atamamak amacıyla refactor edilmeli.

      (redisActor ? FetchRequest(resource.url)).onComplete {
        case Success(value) =>
          value match {
            case FetchResult(result) =>
              result match {
                case Some(topic) =>
                  log.debug(s"Got topic from redis:$topic")
                  val scheduler = context.actorSelection("/user/app/scheduler")
                  scheduler ! Messages.Schedule(resource)

                case None =>
                  val newTopic = UUID.randomUUID().toString
                  log.debug(s"New topic:$newTopic generated for url:${resource.url}")
                  (redisActor ? AddRequest(resource.url, newTopic)).onComplete {
                    case Success(value) => value match {
                      case AddResult(status) if status =>
                        log.debug("Element successfully added to Redis.")
                        val scheduler = context.actorSelection("/user/app/scheduler")
                        scheduler ! Messages.Schedule(resource)

                      case AddResult(status) if !status => log.warning("Element can not added to Redis.")
                    }

                    case Failure(exception) =>
                      sender() ! Messages.JobFailed(
                        job = Job(self, resource),
                        reason = s"An error occurred while adding element to Redis",
                        mayBeThrowable = Some(value = exception)
                      )
                  }
              }
          }

        case Failure(exception) =>
          sender() ! Messages.JobFailed(
            job = Job(self, resource),
            reason = s"An error occurred while getting from Redis",
            mayBeThrowable = Some(value = exception)
          )
      }

    case Messages.Fetch(resource) =>
      log.debug("A new getter creating to fetch resource:{}", resource.toString)
      val getter = context.actorOf(Props(new Getter(resource)))
      context.watch(getter)

    case result: Messages.Result =>
      log.debug("New result received and sending to newly created detector actor.")
      context.stop(context.unwatch(sender))
      val detector = context.actorOf(Props[Detector], s"detector-${Random.nextInt()}")
      context.watch(detector)
      detector ! result

    case uniqueResult: Messages.UniqueResult =>
      log.info(s"Got unique result from ${uniqueResult.resource}")
      context.parent ! uniqueResult

    case Terminated(actor) =>
      log.debug("Actor:{} terminated.", actor.path.toString)

    case ReceiveTimeout =>
      log.error("Got receive timeout! All of the child actors will be stopped.")
      context.children foreach context.stop
  }
}
