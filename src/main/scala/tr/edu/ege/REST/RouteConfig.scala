package tr.edu.ege.REST

import java.util.UUID

import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.{ActorLogging, ActorRef}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{HttpHeader, StatusCodes}
import akka.http.scaladsl.server.{Directives, Route}
import akka.util.Timeout
import spray.json.{DefaultJsonProtocol, RootJsonFormat}
import tr.edu.ege.REST.actorAPI.{PubHandlerApi, UserHandlerApi}
import tr.edu.ege.messages.PubHandler.{ChangeResult, ChangeResults}
import tr.edu.ege.messages.UserHandler.{User, UserCreated, UserNotCreated, UserNotFound, UserResponse}
import tr.edu.ege.models.Resource

import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success}

class RouteConfig(userHandler: ActorRef, pubHandler: ActorRef) extends Directives with JsonProtocol {
  def extractUser(headers: Seq[HttpHeader]): (String, String) = {
    var username = ""
    var password = ""
    headers.foreach {
      header =>
        if (header.name() == "username")
          username = header.value()
        if (header.name() == "password")
          password = header.value()
    }
    (username, password)
  }

  implicit val timeout: Timeout = FiniteDuration(5, "seconds")

  implicit val userApi: UserHandlerApi = new UserHandlerApi(userHandler)
  implicit val pubApi: PubHandlerApi = new PubHandlerApi(pubHandler)

  val pubMedBaseURI = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=pubmed&retmax=999999999&api_key=f3ddb4c1de06900a117c889d4cfbf0666808"

  val userRoutes: Route = pathPrefix("user") {
    post {
      entity(as[User]) { user => {
        path("login") {
          onSuccess(userApi.getUser(user.username)) {
            case UserNotFound =>
              complete(StatusCodes.NotFound -> "User not found.")
            case UserResponse(username, password) =>
              if (password == user.password) {
                userApi.startListen(username)
                complete(StatusCodes.OK -> "Login Successful.")
              }
              else
                complete(StatusCodes.Unauthorized -> "Password is wrong.")
          }
        }
      } ~ path("register") {
        onComplete(userApi.getUser(user.username)) {
          case Success(value) =>
            value match {
              case UserResponse(_, _) =>
                complete(StatusCodes.BadRequest)
              case UserNotFound =>
                onSuccess(userApi.addUser(user.username, user.password)) {
                  case UserCreated => complete(StatusCodes.Created)
                  case UserNotCreated => complete(StatusCodes.BadRequest)
                }
            }
          case Failure(e) =>
            complete(StatusCodes.InternalServerError -> e)
        }
      }
      }
    }

  }

  val pubRoutes: Route = pathPrefix("pub") {
    post {
      entity(as[String]) {
        params =>
          extractRequest {
            req =>
              val (username, password) = extractUser(req.headers)
              onSuccess(userApi.getUser(username)) {
                case UserNotFound =>
                  complete(StatusCodes.Unauthorized)
                case UserResponse(_, pwd) =>
                  if (password != pwd)
                    complete(StatusCodes.Unauthorized)
                  else {
                    path("subscribe") {
                      userApi.registerQuery(username, Resource(s"$pubMedBaseURI&term=$params"))
                      pubApi.startConsumeWithWait(Resource(s"$pubMedBaseURI&term=$params",mayBeQuery = Some(List("IdList", "Id"))), User(username, password))
                      complete(StatusCodes.OK)
                    } ~ path("unsubscribe") {
                      userApi.deregisterQuery(username, Resource(s"$pubMedBaseURI&term=$params"))
                      pubApi.stopConsume(Resource(s"$pubMedBaseURI&term=$params", mayBeQuery = Some(List("IdList", "Id"))), User(username, password))
                      complete(StatusCodes.OK)
                    }

                  }
              }
          }
      }
    } ~
      get {
        extractRequest {
          req =>
            val (username, password) = extractUser(req.headers)
            onSuccess(userApi.getUser(username)) {
              case UserNotFound =>
                complete(StatusCodes.Unauthorized)
              case UserResponse(_, pwd) =>
                if (password != pwd)
                  complete(StatusCodes.Unauthorized)
                else {
                  path("check") {
                    onSuccess(pubApi.checkForUpdates(User(username, password))) {
                      case res : ChangeResults =>
                        complete(res)
                      case _ =>
                        complete(StatusCodes.InternalServerError)
                    }
                  } ~ path("querylist"){
                    onSuccess(userApi.getQueryList(username)) {
                      case res:Set[String] =>
                        complete(res)
                      case _ =>
                        complete(StatusCodes.InternalServerError)
                    }
                  }
                }
            }
        }
      }
  }
}

trait JsonProtocol extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val userFormat: RootJsonFormat[User] = jsonFormat2(User.apply)
  implicit val changeResultFormat: RootJsonFormat[ChangeResult] = jsonFormat2(ChangeResult.apply)
  implicit val changeResultsFormat: RootJsonFormat[ChangeResults] = jsonFormat1(ChangeResults.apply)
}