package tr.edu.ege.client

import java.util.concurrent.Executor

import scala.concurrent.Future

trait WebClient {
  def get(url: String)(implicit exec: Executor): Future[String]
}