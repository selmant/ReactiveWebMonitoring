package tr.edu.ege

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.PathDirectives.pathPrefix
import akka.stream.ActorMaterializer
import tr.edu.ege.REST.{RouteConfig, UserHandler}
import tr.edu.ege.messages.Messages.StartServer

import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success}

class WebServer extends Actor {
  implicit val system: ActorSystem = context.system
  implicit val materialize: ActorMaterializer = ActorMaterializer()(context)
  // needed for the future flatMap/onComplete in the end
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  val userHandler: ActorRef = context.actorOf(Props[UserHandler], "userhandler")
  val routeConfig: RouteConfig = new RouteConfig(userHandler)

  val routes: Route = {
    pathPrefix("api") {
      concat(
        routeConfig.userRoutes
      )
    }
  }

  override def receive: Receive = {
    case StartServer =>
      val futureBinding = Http().bindAndHandle(routes, "localhost", 8080)
      futureBinding.onComplete {
        case Success(binding) =>
          val address = binding.localAddress
          system.log.info("Server online at http://{}:{}/", address.getHostString, address.getPort)
        case Failure(ex) =>
          system.log.error("Failed to bind HTTP endpoint, terminating system", ex)
          system.terminate()
      }
  }
}
