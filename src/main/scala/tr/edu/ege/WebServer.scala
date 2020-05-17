package tr.edu.ege

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.PathDirectives.pathPrefix
import akka.stream.ActorMaterializer
import tr.edu.ege.REST.{RouteConfig, UserHandler}
import tr.edu.ege.messages.Messages.StartServer

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.io.StdIn

class WebServer extends Actor {
    implicit val system: ActorSystem = context.system
    private implicit val dispatcher: ExecutionContextExecutor = system.dispatcher
    private implicit val materialize: ActorMaterializer = ActorMaterializer()(context)

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
            val serverFuture: Future[Http.ServerBinding] = Http().bindAndHandle(routes, "localhost", 4567)

            println("Server started ...")
            StdIn.readLine()
            serverFuture
                .flatMap(_.unbind())
                .onComplete(_ => system.terminate())
    }
}
