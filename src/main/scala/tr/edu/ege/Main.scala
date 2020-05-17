package tr.edu.ege

import akka.actor.{Actor, ActorLogging, ActorRef, Props, ReceiveTimeout}
import tr.edu.ege.actors.db.RedisDbService
import tr.edu.ege.actors.{Checker, Controller, Publisher, Scheduler}
import tr.edu.ege.client.AsyncWebClient
import tr.edu.ege.messages.Messages
import tr.edu.ege.messages.Messages.{StartServer, Submit}
import tr.edu.ege.models.Resource

import scala.concurrent.duration.FiniteDuration

class Main extends Actor with ActorLogging {
  //  if (redisClient.connected) log.info("Redis Client connected successfully...")

  val controller: ActorRef = context.actorOf(Props[Controller], "controller")
  context.watch(controller) // sign death pact

  val checker: ActorRef = context.actorOf(Props[Checker], "checker")
  val scheduler: ActorRef = context.actorOf(Props[Scheduler], "scheduler")
  val publisher: ActorRef = context.actorOf(Props[Publisher], "publisher")

  val redisClient: ActorRef = context.actorOf(Props(new RedisDbService("redis.conf", "scredis", FiniteDuration(5, "seconds"))), "redisActor")

  val webServer: ActorRef = context.actorOf(Props[WebServer], "webserver")
  webServer ! StartServer
  //  context.watch(scheduler)
  val pubMedBaseURI = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=pubmed&retmax=999999999&api_key=f3ddb4c1de06900a117c889d4cfbf0666808"

  controller ! Submit(Resource(s"$pubMedBaseURI&term=asthma[mesh]+AND+leukotrienes[mesh]+AND+2009[pdat]"))
  //  controller ! Submit(Resource(s"$pubMedBaseURI&term=1")) // Raw JSON Data
  //  controller ! Submit(Resource(s"$pubMedBaseURI&term=coronavirus[mesh]")) // Raw JSON Data
  //  controller ! Submit(Resource(s"$pubMedBaseURI&term=protein[mesh]+AND+diet[mesh]")) // Raw JSON Data
  //  controller ! Submit(Resource(s"$pubMedBaseURI&term=high[mesh]+AND+protein[mesh]")) // Raw JSON Data
  //  controller ! Submit(Resource(s"$pubMedBaseURI&term=whey[mesh]+AND+protein[mesh]")) // Raw JSON Data
  //  controller ! Submit(Resource(s"$pubMedBaseURI&term=c-reactive[mesh]+AND+protein[mesh]")) // Raw JSON Data
  //  controller ! Submit(Resource(s"$pubMedBaseURI&term=metabolic[mesh]+AND+syndrome[mesh]")) // Raw JSON Data
  //  controller ! Submit(Resource(s"$pubMedBaseURI&term=nephrotic[mesh]+AND+syndrome[mesh]")) // Raw JSON Data
  //  controller ! Submit(Resource(s"$pubMedBaseURI&term=irritable bowel[mesh]+AND+syndrome[mesh]")) // Raw JSON Data
  //  controller ! Submit(Resource(s"$pubMedBaseURI&term=carpal tunnel[mesh]+AND+syndrome[mesh]")) // Raw JSON Data
  //  controller ! Submit(Resource(s"$pubMedBaseURI&term=infection[mesh]")) // Raw JSON Data
  //  controller ! Submit(Resource(s"$pubMedBaseURI&term=enzyme[mesh]")) // Raw JSON Data
  //  controller ! Submit(Resource(s"$pubMedBaseURI&term=pathogenesis[mesh]")) // Raw JSON Data
  //  controller ! Submit(Resource(s"$pubMedBaseURI&term=immune[mesh]+AND+disease[mesh]")) // Raw JSON Data
  //  controller ! Submit(Resource(s"$pubMedBaseURI&term=bipolar[mesh]+AND+disease[mesh]")) // Raw JSON Data
  //  controller ! Submit(Resource(s"$pubMedBaseURI&term=alzheimer[mesh]+AND+disease[mesh]")) // Raw JSON Data
  //  controller ! Submit(Resource(s"$pubMedBaseURI&term=parkinson[mesh]+AND+disease[mesh]")) // Raw JSON Data
  //  controller ! Submit(Resource(s"$pubMedBaseURI&term=celiac[mesh]+AND+disease[mesh]")) // Raw JSON Data

  //  context.setReceiveTimeout(100.seconds)

  def receive: Receive = {
    case Messages.UniqueResult(job, payload) =>
      if (payload.isEmpty) {
        log.error("No results found for job:'{}'", job)
      } else {
        log.info("Results for job:'{}':{}", job, payload)
      }

    case Messages.JobFailed(job, reason, throwable) =>
      log.info(s"Failed to fetch job:'$job': $reason\n")

    case ReceiveTimeout =>
      log.error("Got receive timeout.")
      context.stop(self)
  }

  override def postStop(): Unit = {
    AsyncWebClient.shutdown()
  }
}