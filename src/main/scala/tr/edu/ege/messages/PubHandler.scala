package tr.edu.ege.messages

import tr.edu.ege.messages.UserHandler.User
import tr.edu.ege.models.Resource

object PubHandler {
  case class SubscribePub(resource: Resource, user: User) extends Message
}
