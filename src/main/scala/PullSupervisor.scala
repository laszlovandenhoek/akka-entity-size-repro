import akka.Done
import akka.actor.{Actor, ActorRef, ActorSystem, PoisonPill, Props}
import com.typesafe.config.Config

class PullSupervisor(config: Config, system: ActorSystem, feedUrl: String) extends Actor {
    private val log = org.log4s.getLogger

    def receive = {
        case PullSupervisor.StartPull =>
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
            initiator ! Done

        case msg: PullFailure =>
            log.debug(msg.toString)
            self ! PoisonPill
            initiator ! Done
    }
}

object PullSupervisor {
    case object StartPull

    def props(config: Config, system: ActorSystem, feedUrl: String) =
        Props(new PullSupervisor(config, system, feedUrl))
}
