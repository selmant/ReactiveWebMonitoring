package tr.edu.ege.actors.db

import scala.concurrent.duration.FiniteDuration

trait RedisDbT {
  val configName: String
  val path: String
  val timeout: FiniteDuration
}

object RedisDbT {

  case class AddRequest(key: String, value: String, duration: Option[FiniteDuration] = None)

  case class AddResult(status: Boolean)

  case class DeleteRequest(key: String)

  case class DeleteResult(status: Boolean)

  case class FetchRequest(key: String)

  case class FetchResult(result: Option[String])

  case class EnqueueRequest(queueName: String, value: String, duration: Option[FiniteDuration] = None)

  case class EnqueueResult(status: Boolean)

  case class DequeueRequest(queueName: String)

  case class DequeueResult(result: Option[String])

  case class AddInSetRequest(key: String, value: String)

  case class GetSetRequest(key: String)

}