package tr.edu.ege.REST

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directives, Route}
import akka.util.Timeout
import tr.edu.ege.messages.UserHandler.JsonProtocol

import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success}

class RouteConfig(userHandler: ActorRef) extends Directives with JsonProtocol {

    implicit val timeout: Timeout = FiniteDuration(5, "seconds")

    implicit val userApi :UserHandlerApi = new UserHandlerApi(userHandler)

    lazy val userRoutes: Route = pathPrefix("user" / Segment) { username ⇒
        get {
            pathEndOrSingleSlash {
                onComplete(userApi.getUser(username)) {
                    case Success(value) =>
                        value match {
                            case Some(user) =>
                                complete(user)
                            case None =>
                                complete(StatusCodes.NotFound)
                        }
                    case Failure(exception) =>
                        complete(StatusCodes.BadGateway)
                }
            }
        }
    }
}
