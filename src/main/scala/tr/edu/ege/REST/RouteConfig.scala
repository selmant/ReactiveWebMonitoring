package tr.edu.ege.REST

import akka.actor.{ActorLogging, ActorRef}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directives, Route}
import akka.util.Timeout
import tr.edu.ege.messages.UserHandler.{JsonProtocol, User, UserAlreadyExist, UserCreated}

import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success}

class RouteConfig(userHandler: ActorRef) extends Directives with JsonProtocol {

    implicit val timeout: Timeout = FiniteDuration(5, "seconds")

    implicit val userApi: UserHandlerApi = new UserHandlerApi(userHandler)

    protected val getUserRoute: Route = pathPrefix("user" / Segment) { username ⇒
        get {
            pathEndOrSingleSlash {
                onComplete(userApi.getUser(username)) {
                    case Success(user) =>
                        if (user.username == "" & user.password == "")
                            complete(StatusCodes.NotFound)
                        else
                            complete(user)
                    case Failure(exception) =>
                        complete(StatusCodes.InternalServerError -> exception)
                }
            }
        }
    }

    protected val createUserRoute: Route = {
        pathPrefix("user" / "create") {
            post {
                pathEndOrSingleSlash {
                    entity(as[User]) { user =>
                        onComplete(userApi.getUser(user.username)) {
                            case Success(usr) =>
                                if (usr.username != "" | usr.password != "")
                                    complete(StatusCodes.BadRequest)
                                else
                                    onSuccess(userApi.addUser(user.username, user.password)) {
                                        case UserCreated => complete(StatusCodes.OK)
                                        case UserAlreadyExist => complete(StatusCodes.BadRequest)
                                    }
                        }
                    }
                }
            }
        }
    }

    val userRoutes: Route = getUserRoute ~ createUserRoute
}
