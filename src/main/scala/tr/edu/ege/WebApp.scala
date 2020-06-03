package tr.edu.ege

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.stream.ActorMaterializer
import akka.util.Timeout
import tr.edu.ege.actors.db.RedisDbService
import tr.edu.ege.actors.{Checker, Controller, Publisher, Scheduler}
import tr.edu.ege.messages.Messages.StartServer

import scala.concurrent.duration.FiniteDuration

object WebApp extends App {
  implicit val system: ActorSystem = ActorSystem("Main")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val timeout: Timeout = FiniteDuration(5, "seconds")

  val mainApp: ActorRef = system.actorOf(Props[Main], "app")
  val webServer: ActorRef = system.actorOf(Props[WebServer], "webserver")
  webServer ! StartServer

}
