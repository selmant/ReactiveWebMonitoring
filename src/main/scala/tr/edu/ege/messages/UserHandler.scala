package tr.edu.ege.messages

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{DefaultJsonProtocol, RootJsonFormat}
import tr.edu.ege.models.Resource

object UserHandler {

    case class User(username: String, password: String)

    case class AddUser(username: String, password: String) extends Message
    case class GetUser(username: String) extends Message
    case class RegisterQuery(username: String, resource: Resource) extends Message
    case class DeregisterQuery(username: String, resource: Resource) extends Message
    case class GetQueryList(username:String) extends Message
    case class StopListen(username:String) extends Message
    case class StartListen(username:String) extends Message

    sealed trait UserRegisterResponse

    case object UserCreated extends UserRegisterResponse
    case object UserNotCreated extends UserRegisterResponse

    sealed trait UserLoginResponse

    case class UserResponse(username:String, password:String) extends UserLoginResponse
    case object UserNotFound extends UserLoginResponse

}
