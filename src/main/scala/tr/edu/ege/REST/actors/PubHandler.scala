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
  implicit val timeout: Timeout = FiniteDuration(20, "seconds")

  override def receive: Receive = LoggingReceive {
    case StartConsumeWithWait(resource, user) =>
      val controller = context.actorSelection("/user/app/controller")
      val scheduler = context.actorSelection("/user/app/scheduler")
      controller ! Submit(resource)
      (scheduler ? Started(resource)).onComplete {
        case Success(value) =>
          value match {
            case true =>
              self ! StartConsume(resource, user)
            case false =>
              context.system.scheduler.scheduleOnce(1 second, self, StartConsumeWithWait(resource, user))
          }
      }
    case StartConsume(resource, user) =>
      Thread.sleep(2000)
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
      (redisActor ? GetSetRequest(s"user:${user.username}:topics")).onComplete {
        case Success(value) => value match {
          case GetSetResult(topicUrls) =>
            for (topicUrl <- topicUrls) {
              (redisActor ? FetchRequest(topicUrl)).onComplete {
                case Success(value) =>
                  value match {
                    case FetchResult(result) =>
                      result match {
                        case Some(topic) =>
                          val maybeActor: Option[ActorRef] = context.child(topic)
                          maybeActor match {
                            case Some(actor) =>
                              (actor ? CheckForChanges(user)).onComplete {
                                case Success(value) => value match {
                                  case result: ChangeResult =>
                                    resultSet += result
                                  case None =>
                                }
                              }
                            case None => log.error(s"Actor Not found for $topic")
                          }
                      }
                  }
              }
            }
            sender() ! ChangeResults(resultSet.toSet)
        }
        case Failure(exception) => throw exception
      }
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

  def futureToFutureTry[T](f: Future[T]): Future[Try[T]] = f.map(Success(_)).recover(x => Failure(x))


}