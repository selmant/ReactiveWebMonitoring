package tr.edu.ege.models

import tr.edu.ege.messages.Gettable
import tr.edu.ege.messages.Gettable

case class Resource(url: String, mayBeQuery: Option[List[String]] = None) extends Gettable
