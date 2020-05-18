package tr.edu.ege.REST

import akka.actor.{ActorLogging, ActorRef}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directives, Route}
import akka.util.Timeout
import tr.edu.ege.messages.PubHandler.SubscribePub
import tr.edu.ege.messages.UserHandler.{JsonProtocol, User, UserCreated, UserNotCreated}
import tr.edu.ege.models.Resource

import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success}

class RouteConfig(userHandler: ActorRef, pubHandler: ActorRef) extends Directives with JsonProtocol {

  implicit val timeout: Timeout = FiniteDuration(5, "seconds")

  implicit val userApi: UserHandlerApi = new UserHandlerApi(userHandler)
  implicit val pubApi: PubHandlerApi = new PubHandlerApi(pubHandler)

  val pubMedBaseURI = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=pubmed&retmax=999999999&api_key=f3ddb4c1de06900a117c889d4cfbf0666808"

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
                    case UserCreated => complete(StatusCodes.Created)
                    case UserNotCreated => complete(StatusCodes.BadRequest)
                  }
            }
          }
        }
      }
    }
  }

  protected val subscribePub: Route = {
    pathPrefix("pub" / "subscribe") {
      post {
        pathEndOrSingleSlash {
          entity(as[String]) { params =>
            extractRequest { req =>
              var username = ""
              var password = ""
              req.headers.foreach { header =>
                if (header.name() == "username")
                  username = header.value()
                if (header.name() == "password")
                  password = header.value()
              }
              onComplete(userApi.getUser(username)) {
                case Success(user) =>
                  if (user.username == "" & user.password == "")
                    complete(StatusCodes.Unauthorized)
                  else if (user.password != password)
                    complete(StatusCodes.Unauthorized)
                  else {
                    pubApi.subscribePub(Resource(s"$pubMedBaseURI&term=$params"), User(username, password))
                    complete(StatusCodes.OK)
                  }
              }
            }
          }
        }
      }
    }
  }

  val userRoutes: Route = getUserRoute ~ createUserRoute
  val pubRoutes: Route = subscribePub
}
