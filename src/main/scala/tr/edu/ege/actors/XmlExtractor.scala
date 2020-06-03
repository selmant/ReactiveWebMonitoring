package tr.edu.ege.actors

import akka.actor.{Actor, ActorLogging, Props}
import akka.event.LoggingReceive
import akka.pattern.ask
import akka.util.Timeout
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._
import tr.edu.ege.actors.db.RedisDbT.{AddRequest, AddResult, FetchRequest, FetchResult}
import tr.edu.ege.messages.Messages
import tr.edu.ege.messages.Messages.ExtractXMLResult
import tr.edu.ege.messages.TopicConsumer.PushNewChanges
import tr.edu.ege.models.Resource

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success}
import scala.xml.{Elem, NodeSeq, XML}

class XmlExtractor extends Actor with ActorLogging {
  implicit val timeout: Timeout = FiniteDuration(5, "seconds")
  private val redisActor = context.system.actorSelection("user/app/redisActor")

  override def receive: Receive = LoggingReceive {
    case result: ExtractXMLResult =>
      val topicConsumer = context.system.actorSelection(s"/user/webserver/pubhandler/${result.topic}")
      val xmlString = result.payload
      val xml: Elem = XML.loadString(xmlString)
      val query = result.resource.mayBeQuery.get
      val ns: NodeSeq = xml
      val idElems: NodeSeq = query.foldLeft(ns)(op = (e, s) => e \ s)
      val currentIds: Set[Int] = idElems.map(_.text.toInt).toSet
      log.info(s"Current ids extracted.[$currentIds]")

      val key = result.resource.asJson.noSpaces
      //      TODO consider using this style
      //      val eventualIds = ask(redisActor, FetchElementRequest(key)    ).mapTo[String]
      log.info(s"Getting redis value of $key")
      (redisActor ? FetchRequest(key)).onComplete {
        case Success(value) => value match {
          case FetchResult(res) =>
            res match {
              case Some(idsJsonString) =>
                log.debug(s"Found a value at Redis with $key")
                decode[Set[Int]](idsJsonString) match {
                  case Right(idsSet) =>
                    log.info(s"Last ids:${idsSet.toString} decoded from json")
                    val differences: Set[Int] = currentIds.diff(idsSet)
                    topicConsumer ! PushNewChanges(differences.to(mutable.Set))
                  case Left(error) => log.error(error.getMessage)
                }

              case None =>
                log.warning(s"No Redis value found with $key")
                topicConsumer ! PushNewChanges(currentIds.to(mutable.Set))
            }
        }
        case Failure(exception) => throw exception
      }

      // Set new publication ids to the Redis as a last value
      (redisActor ? AddRequest(key, currentIds.asJson.noSpaces)).onComplete {
        case Success(value) => value match {
          case AddResult(res) if res => log.debug("Element successfully added to Redis.")
          case AddResult(res) if !res => log.error("An error occurred while adding element to Redis.")
        }
        case Failure(exception) => throw exception
      }

    case result: Messages.Result =>
      log.info("New publication abstract extracted")
      log.info(result.toString)

  }

  private def getPublicationAbstract(id: Int) = {
    val url = s"https://www.ncbi.nlm.nih.gov/pubmed/$id?report=abstract&format=text"
    log.info(s"Url:$url forwarding to the Getter Actor.")
    val resource = Resource(url, None)
    context.actorOf(Props(new Getter(resource)))
  }
}
