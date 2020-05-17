package tr.edu.ege.messages

import tr.edu.ege.models.{Job, Resource}

object Messages {

  case class Submit(resource: Resource) extends Message

  case class Check(resource: Resource) extends Message

  case class Fetch(resource: Resource) extends Message

  case class Result(resource: Resource, payload: String) extends Message
  case class ExtractJsonResult(resource: Resource, payload: String) extends Message
  case class ExtractHTMLResult(resource: Resource, payload: String) extends Message
  case class ExtractXMLResult(resource: Resource, payload: String) extends Message

  object UniqueResult {
    def fromResult(result: Result): UniqueResult = {
      UniqueResult(result.resource, result.payload)
    }
  }

  case class UniqueResult(resource: Resource, payload: String) extends Message

  case class JobFailed(job: Job, reason: String, mayBeThrowable: Option[Throwable] = None) extends Message

  //  case class Failed(reason: String, mayBeThrowable: Option[Throwable] = None) extends Message

  //  case class Subscribe(channel: String) extends Message
  //
  //  case class Register(callback: PubSubMessage => Any) extends Message
  //
  //  case class Unsubscribe(channels: Array[String]) extends Message
  //
  //  case object UnsubscribeAll extends Message

  case class Publish(topic: String = "url", key: String, value: String) extends Message

  case class Elements(contents: Set[String]) extends Message

  case class Schedule(resource: Resource) extends Message

  case class StartServer() extends Message

}
