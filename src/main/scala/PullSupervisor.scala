import akka.actor.{Actor, ActorRef, PoisonPill, Props}
import com.typesafe.config.Config

class PullSupervisor(config: Config, feedUrl: String) extends Actor {
    case object StartPull

    private val log = org.log4s.getLogger

    override def preStart(): Unit = {
        self ! StartPull
    }

    def receive = {
        case StartPull =>
            log.debug(s"Received pull request")
            val maxFeedSize = config.getMemorySize("livefeeds.max-feed-size").toBytes
            context.actorOf(PullWorker.props(feedUrl, maxFeedSize))
            context become inProgress(sender())
    }

    def inProgress(initiator: ActorRef): Receive = {
        case msg: PullStart =>
            log.debug(msg.toString)

        case msg: PullProgress =>
            log.debug(msg.toString)

        case msg: PullSummary =>
            log.debug(msg.toString)
            self ! PoisonPill

        case msg: PullFailure =>
            log.debug(msg.toString)
            self ! PoisonPill
    }
}

object PullSupervisor {
    def props(config: Config, feedUrl: String) = Props(new PullSupervisor(config, feedUrl))
}
