package tr.edu.ege.messages

import tr.edu.ege.messages.UserHandler.User

import scala.collection.mutable

object TopicConsumer {
  case class AddListener(user:User) extends Message
  case class RemoveListener(user:User) extends Message
  case class PushNewChanges(changes: mutable.Set[Int]) extends Message
  case class CheckForChanges(user: User) extends Message
  case class CheckLoggedOut(changeId: Int) extends Message
}
