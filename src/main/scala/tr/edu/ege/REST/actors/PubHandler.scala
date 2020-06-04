package tr.edu.ege.REST.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.event.LoggingReceive
import akka.pattern.ask
import akka.util.Timeout
import tr.edu.ege.TopicConsumer
import tr.edu.ege.actors.db.RedisDbT._
import tr.edu.ege.messages.Messages.{Started, Submit}
import tr.edu.ege.messages.PubHandler.{ChangeResult, ChangeResults, CheckForUpdates, StartConsume, StartConsumeWithWait, StopConsume}
import tr.edu.ege.messages.TopicConsumer.{AddListener, CheckForChanges, RemoveListener}
import tr.edu.ege.messages.UserHandler.User

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success, Try}
import scala.concurrent.duration._

class PubHandler extends Actor with ActorLogging {

  private val redisActor = context.actorSelection("/user/app/redisActor")
  implicit val timeout: Timeout = FiniteDuration(5, "seconds")

  override def receive: Receive = LoggingReceive {
    case StartConsumeWithWait(resource, user) =>
      val controller = context.actorSelection("/user/app/controller")
      controller ! Submit(resource)
      self ! StartConsume(resource, user)

    case StartConsume(resource, user) =>
      val scheduler = context.actorSelection("/user/app/scheduler")
      (scheduler ? Started(resource)).onComplete {
        case Success(value) =>
          value match {
            case true =>
              (redisActor ? FetchRequest(resource.url)).onComplete {
                case Success(value) =>
                  value match {
                    case FetchResult(result) =>
                      result match {
                        case Some(topic) =>
                          val maybeActor: Option[ActorRef] = context.child(topic)
                          maybeActor match {
                            case Some(actor) =>
                              log.debug(s"TopicConsumer not added for $topic")
                              actor ! AddListener(user)
                            case None =>
                              log.debug(s"new TopicConsumer created $topic")
                              val newActor = context.actorOf(Props(new TopicConsumer(resource)), topic)
                              newActor ! AddListener(user)
                          }
                          log.debug("User is added to consumer")
                        case None => throw new Exception
                      }

                  }
              }
            case false =>
              context.system.scheduler.scheduleOnce(500 millisecond, self, StartConsume(resource, user))
          }
      }
    case StopConsume(resource, user) =>
      (redisActor ? FetchRequest(resource.url)).onComplete {
        case Success(value) =>
          value match {
            case FetchResult(result) =>
              result match {
                case Some(topic) =>
                  val maybeActor: Option[ActorRef] = context.child(topic)
                  maybeActor match {
                    case Some(actor) =>
                      log.debug(s"${user.username} stopped listen to $topic TopicConsumer.")
                      actor ! RemoveListener(user)
                  }
                  log.debug("User is removed from consumer")
                case None => log.warning("Topic could not found on Redis.")
              }

          }
      }
    case CheckForUpdates(user) =>
      var resultSet: mutable.Set[ChangeResult] = mutable.Set()
      val topicSetFuture = redisActor ? GetSetRequest(s"user:${user.username}:topics")
      val topicSetResult = Await.result(topicSetFuture, 1 second).asInstanceOf[GetSetResult]
      topicSetResult match {
        case GetSetResult(topicUrls) =>
          for (topicUrl <- topicUrls) {
            val maybeTopic = topicFromUrl(topicUrl)
            maybeTopic match {
              case Some(topic) =>
                val maybeActor: Option[ActorRef] = context.child(topic)
                maybeActor match {
                  case Some(actor) =>
                    val changeSetFuture = actor ? CheckForChanges(user)
                    val changeSetResult = Await.result(changeSetFuture, 1 second).asInstanceOf[Option[ChangeResult]]
                    changeSetResult match {
                      case Some(result) =>
                        resultSet += result
                      case None =>
                        log.debug(s"No change found for $topic")
                    }
                  case None => log.error(s"Actor Not found for $topic")
                }
            }
          }
      }
      sender() ! ChangeResults(resultSet.toSet)
  }

  def topicFromUrl(url: String): Option[String] = {
    val future = redisActor ? FetchRequest(url)
    val result = Await.result(future, timeout.duration).asInstanceOf[FetchResult]
    result match {
      case FetchResult(result) =>
        result match {
          case Some(topic) =>
            Some(topic)
        }
    }
  }
}