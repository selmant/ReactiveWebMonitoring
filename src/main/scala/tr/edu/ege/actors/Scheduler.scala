package tr.edu.ege.actors

import akka.actor.{Actor, ActorLogging}
import akka.event.LoggingReceive
import com.typesafe.akka.extension.quartz.QuartzSchedulerExtension
import tr.edu.ege.messages.Messages
import tr.edu.ege.messages.Messages.Schedule

class Scheduler extends Actor with ActorLogging {

  val quartzScheduler: QuartzSchedulerExtension = QuartzSchedulerExtension(context.system)

  override def receive: Receive = LoggingReceive {
    case m: Schedule =>
      try {
        val jobName = m.resource.url
        val schedule = quartzScheduler.createJobSchedule(
          name = jobName,
          receiver = sender(),
          msg = Messages.Fetch(m.resource),
          cronExpression = "*/2 * * ? * *" // Will fire every 20 seconds
        )
        log.info("Job: {} scheduled at: {}.", jobName, schedule)
        log.debug("Running schedule jobs: {}", quartzScheduler.runningJobs)
      } catch {
        case exception: IllegalArgumentException =>
          exception.printStackTrace()
        //          context.parent ! Messages.JobFailed(job, reason = "Illegal argument", Some(exception))
      }
  }
}
