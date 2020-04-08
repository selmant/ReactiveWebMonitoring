package tr.edu.ege.client

import java.util.concurrent.Executor

import scala.concurrent.ExecutionContext

object WebClientTest extends App {
  implicit val executor: Executor with ExecutionContext = scala.concurrent.ExecutionContext.global.asInstanceOf[Executor with ExecutionContext]

  AsyncWebClient get "http://www.google.com/1" map println andThen { case _ => AsyncWebClient.shutdown() }
}