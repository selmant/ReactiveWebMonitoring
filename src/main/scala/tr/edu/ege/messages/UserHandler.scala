package tr.edu.ege.messages

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

object UserHandler {

    case class User(username: String, password: String)

    case class AddUser(username: String, password: String) extends Message
    case class UpdateUser(username: String) extends Message
    case class GetUser(username: String) extends Message
    case class DeleteUser(username: String) extends Message

    sealed trait UserResponse

    case object UserCreated extends UserResponse
    case object UserNotCreated extends UserResponse

    trait JsonProtocol extends SprayJsonSupport with DefaultJsonProtocol {
        implicit val userFormat: RootJsonFormat[User] = jsonFormat2(User.apply)
    }

}
