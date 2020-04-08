package tr.edu.ege.actors

import akka.actor.{Actor, ActorLogging, OneForOneStrategy, ReceiveTimeout, SupervisorStrategy, Terminated}
import akka.event.LoggingReceive
import tr.edu.ege.messages.Messages
import tr.edu.ege.messages.Messages.JobFailed

class Controller extends Actor with ActorLogging {

  override val supervisorStrategy: OneForOneStrategy = OneForOneStrategy(maxNrOfRetries = 5) {
    case _: Exception => SupervisorStrategy.Restart
  }

//  context.setReceiveTimeout(100.seconds)

  def receive: Receive = LoggingReceive {
    case Messages.Submit(resource) =>
      log.debug("Sending resource:{} to checker actor.", resource.toString)
      val checker = context.actorSelection("/user/app/checker")
//      context.watch(checker)
      checker ! Messages.Check(resource)

    case Terminated(actor) =>
      log.error("Actor:{} terminated.", actor.path.toString)
      if (context.children.isEmpty) {
        log.debug("Controller context has no children. Controller stopping itself.")
        context.stop(self)
      }

    case ReceiveTimeout =>
      log.error("Got ReceiveTimeout, all of the child actors will be stopped.")
      context.children foreach context.stop

    case err: JobFailed =>
      log.error(s"Job failed. Details: $err")

    case Messages.UniqueResult(job, payload) =>
      if (payload.isEmpty) {
        log.error("No results found for job:'{}'", job)
      } else {
        log.info("Results for job:'{}':{}", job, payload)
      }
  }
}