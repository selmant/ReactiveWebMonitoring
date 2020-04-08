package tr.edu.ege.actors

import akka.actor.{Actor, ActorLogging}
import akka.event.LoggingReceive
import org.jsoup.Jsoup
import org.jsoup.select.Elements
import tr.edu.ege.messages.Messages.ExtractHTMLResult
import tr.edu.ege.models.Resource

import scala.collection.JavaConverters._

class HtmlExtractor extends Actor with ActorLogging {

  override def receive: Receive = LoggingReceive {
    case htmlResult: ExtractHTMLResult =>
      val resource = htmlResult.resource
      val payload = htmlResult.payload
      val contents = if (resource.mayBeQuery.isDefined) {
        val elements = findElements(payload, resource)
        log.info(s"Found ${elements.size} elements from resource:'${resource.toString}'")
        elements.toSet
      } else {
        log.warning("Query not found!")
        Set(payload)
      }

      //      context.parent ! Messages.Elements(contents)
      log.info("Extraction completed successfully. Elements: {}", contents.mkString("(", "\n", ")"))
  }

  def findElements(body: String, resource: Resource): List[String] = {
    val document = Jsoup.parse(body, resource.url)
    val query = resource.mayBeQuery.get.mkString(" ")
    val elements: Elements = document.select(query)
    for {
      element <- elements.iterator().asScala.toList
    } yield element.html()
  }
}
