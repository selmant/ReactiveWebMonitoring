package tr.edu.ege.messages

import tr.edu.ege.messages.UserHandler.User
import tr.edu.ege.models.Resource

object PubHandler {

  case class StartConsume(resource: Resource, user: User) extends Message

  case class StopConsume(resource: Resource, user: User) extends Message

  case class CheckForUpdates(user: User) extends Message

  case class ChangeResult(url: String, changes: Set[Int])

  case class ChangeResults(changes: Set[ChangeResult])

  case class StartConsumeWithWait(resource: Resource, user: User) extends Message

}
