package tr.edu.ege.client

import java.util.concurrent.Executor

import com.ning.http.client.AsyncHttpClient
import com.typesafe.scalalogging.LazyLogging
import tr.edu.ege.client.exceptions.BadStatus

import scala.concurrent.{Future, Promise}

object AsyncWebClient extends WebClient with LazyLogging{

  private val client = new AsyncHttpClient

  def get(url: String)(implicit exec: Executor): Future[String] = {
    logger.info(s"Request preparing for url:$url")
    val f = client.prepareGet(url)
      .addHeader("user-agent","Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.88 Safari/537.36")
      .execute()
    val p = Promise[String]()
    f.addListener(() => {
      val response = f.get
      val statusCode = response.getStatusCode
      logger.info(s"Got status:$statusCode from url:$url")
      if (statusCode / 100 < 4)
        p.success(response.getResponseBodyExcerpt(131072))
      else p.failure(BadStatus(statusCode))
    }, exec)
    p.future
  }

  def shutdown(): Unit = client.close()

}

