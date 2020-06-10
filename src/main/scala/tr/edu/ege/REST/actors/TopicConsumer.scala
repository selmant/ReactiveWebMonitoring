package tr.edu.ege

import java.util.UUID

import scala.language.postfixOps
import akka.actor.{Actor, ActorContext, ActorRef, ActorSelection, ActorSystem, Props}
import akka.event.LoggingReceive
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import tr.edu.ege.actors.ResourceConsumer
import tr.edu.ege.actors.ResourceConsumer._
import tr.edu.ege.actors.db.RedisDbT.{FetchRequest, FetchResult}
import tr.edu.ege.configuration.Configuration
import tr.edu.ege.messages.PubHandler.ChangeResult
import tr.edu.ege.messages.TopicConsumer._
import tr.edu.ege.messages.UserHandler.{StopListen, User}
import tr.edu.ege.models.Resource

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class TopicConsumer(resource: Resource) extends Actor with LazyLogging {

  implicit val system: ActorSystem = context.system
  implicit val materialize: ActorMaterializer = ActorMaterializer()(context)
  // needed for the future flatMap/onComplete in the end
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  implicit val timeout: Timeout = FiniteDuration(5, "seconds")
  implicit val ctx: ActorContext = context

  private val redisActor = context.actorSelection("/user/app/redisActor")
  implicit val userActor: ActorSelection = context.actorSelection("/user/webserver/userhandler")

  val userSet: mutable.Set[User] = mutable.Set()
  val changes: mutable.Set[Change] = mutable.Set()


  private val url = resource.url
  private val query = resource.mayBeQuery
  logger.info(s"TopicConsumer started for $url")


  private val consumerActor = context.actorOf(
    props = Props(new ResourceConsumer(Resource(url, query))),
    name = "consumer-actor"
  )

  private val consumerSink = Sink.actorRefWithBackpressure(
    ref = consumerActor,
    onInitMessage = StreamInitialized,
    ackMessage = Ack,
    onCompleteMessage = StreamCompleted,
    onFailureMessage = StreamFailure
  )

  private val consumerSettings = ConsumerSettings(system, new StringDeserializer, new StringDeserializer)
    .withBootstrapServers(s"${Configuration.KAFKA_HOST}:${Configuration.KAFKA_PORT}")
    .withGroupId(s"consumer-group-${UUID.randomUUID()}")
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")


  (redisActor ? FetchRequest(url)).onComplete {
    case Success(value) => value match {
      case FetchResult(maybeTopic) => maybeTopic match {
        case Some(topic) =>
          logger.info(s"Kafka consumer starting for topic: $topic")
          val runningStream: Consumer.Control = Consumer.plainSource(
            settings = consumerSettings.withClientId(s"client-${UUID.randomUUID()}"),
            subscription = Subscriptions.topics(topic)
          )
            //    .filter { message: ConsumerRecord[String, String] => message.key() == url } TODO key kontrolü eklenecek
            .to(consumerSink)
            .run()
        case None => throw new IllegalStateException("Topic id not found at Redis")
      }
    }
    case Failure(exception) => throw exception
  }


  override def receive: Receive = LoggingReceive {
    case AddListener(user) =>
      userSet += user
    case RemoveListener(user) =>
      userSet -= user
      if (userSet.isEmpty) {
        context.stop(self)
      }
    case PushNewChanges(changeSet) =>
      changes += new Change(changeSet, userSet.clone(), self)
    case CheckForChanges(user) =>
      val currentSender = sender()
      var completeChanges: Set[Int] = Set()
      for(change<-changes){
        val set = change.getChanges(user)
        completeChanges ++= set
      }
      changes.filterInPlace(!_.isDone)
      if (completeChanges.nonEmpty) {
        currentSender ! Some(ChangeResult(url, completeChanges))
      } else {
        logger.debug("Nothing found at TopicConsumer")
        currentSender ! None
      }

    case CheckLoggedOut(id) =>
      changes.foreach(change => {
        if (id == change.id) {
          change.logOut()
        }
      })
      changes.filterInPlace(!_.isDone)
  }
}

object Change {
  private var count: Int = 0

  def getID: Int = {
    count = count + 1
    count
  }
}

class Change(changeSet: mutable.Set[Int], users: mutable.Set[User], topic: ActorRef)(implicit val userActor: ActorSelection, implicit val context: ActorContext) {

  import Change.getID

  val id: Int = getID

  context.system.scheduler.scheduleOnce(5 minutes, topic, CheckLoggedOut(id))


  def isDone: Boolean = users.isEmpty

  def getChanges(user: User): mutable.Set[Int] = {
    for(usr<-users){
      if(user.username == usr.username){
        users -= usr
        return changeSet.clone()
      }
    }
    mutable.Set()
  }

  def logOut(): Unit = {
    users.foreach(user => {
      userActor ! StopListen(user.username)
    })
    users.clear()
  }
}