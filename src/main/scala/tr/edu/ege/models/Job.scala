package tr.edu.ege.models

import akka.actor.ActorRef

case class Job(client: ActorRef, resource: Resource)
