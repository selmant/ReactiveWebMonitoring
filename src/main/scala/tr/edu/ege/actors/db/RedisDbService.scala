package tr.edu.ege.actors.db

import akka.actor.{Actor, ActorLogging}
import scredis._

import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success}

class RedisDbService(val configName: String, val path: String, val timeout: FiniteDuration) extends Actor with ActorLogging {

  import RedisDbT._

  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val client = Redis(configName, path)

  implicit val _timeout: FiniteDuration = timeout

  override def receive: Receive = {
    case req: AddRequest =>
      val currentSender = sender()
      val addFut = req.duration match {
        case Some(dur) =>
          client.set(
            key = req.key,
            value = req.value,
            ttlOpt = Some(dur)
          )
        case None =>
          client.set(
            key = req.key,
            value = req.value
          )
      }
      addFut.onComplete {
        case Success(res) =>
          currentSender ! AddResult(res)
        case Failure(ex) =>
          log.error(ex,"Error Adding To Redis : {}", ex.getMessage)
          currentSender ! AddResult(false)
      }

    case req: DeleteRequest =>

      val currentSender = sender()
      val delFut = client.del(req.key)
      delFut.onComplete {
        case Success(res) => res match {
          case x if x < 1 => currentSender ! DeleteResult(false)
          case _ => currentSender ! DeleteResult(true)
        }
        case Failure(ex) =>
          log.error(ex,"Error Deleting from Redis : {}", ex.getMessage)
          currentSender ! DeleteResult(true)
      }

    case req: FetchRequest =>
      val currentSender = sender()
      val fetchFut = client.get(req.key)
      fetchFut.onComplete {
        case Success(res: Option[String]) =>
          currentSender ! FetchResult(res.map((a: String) => a))
        case Failure(ex) =>
          log.error(ex,"Error Fetching from Redis : {}", ex.getMessage)
          currentSender ! FetchResult(None)
      }

    case req: EnqueueRequest =>
      val currentSender = sender()

      val enqueueFut = client.rPush(req.queueName, req.value)
      enqueueFut.onComplete {
        case Success(res) =>
          res match {
            case x if x < 1 => currentSender ! EnqueueResult(false)
            case _ => currentSender ! EnqueueResult(true)
          }
        case Failure(ex) =>
          log.error(ex,"Error Enqueuing in Redis : {}", ex.getMessage)
          currentSender ! EnqueueResult(false)
      }

    case req: DequeueRequest =>
      val currentSender = sender()
      val dequeueFut = client.lPop(req.queueName)
      dequeueFut.onComplete {
        case Success(res) =>
          currentSender ! DequeueResult(res)
        case Failure(ex) =>
          log.error(ex,"Error Dequeuing in Redis : {}", ex.getMessage)
          currentSender ! DequeueResult(None)
      }
  }

  override def postStop(): Unit = {
    client.quit()
  }
}