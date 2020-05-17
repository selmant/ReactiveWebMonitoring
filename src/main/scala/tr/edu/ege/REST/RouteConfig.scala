package tr.edu.ege.REST

import akka.actor.ActorRef
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.{Directives, Route}
import akka.pattern.ask
import akka.util.Timeout
import tr.edu.ege.messages.UserHandler
import tr.edu.ege.messages.UserHandler.{GetUser, JsonProtocol}

import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success}

class RouteConfig(userHandler: ActorRef) extends Directives with JsonProtocol {

  implicit val timeout: Timeout = FiniteDuration(5, "seconds")

  implicit val userApi: UserHandlerApi = new UserHandlerApi(userHandler)

  lazy val userRoutes: Route =

    pathPrefix("user" / Segment) { username =>
      concat(
        get {
          val eventualMaybeUser = (userHandler ? GetUser(username)).mapTo[Option[UserHandler.User]]

          onComplete(eventualMaybeUser) {
            case Failure(exception) => complete(StatusCodes.InternalServerError)
            case Success(maybeUser) => maybeUser match {
              case Some(user) =>
                complete(HttpResponse(entity = s"Hello ${user.username}"))
              case None =>
                complete(StatusCodes.NotFound, s"User:$username could not found at system.")
            }
          }
        }
      )
    }
}
