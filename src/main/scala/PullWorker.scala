import java.time.Instant
import scala.util.{Failure, Success}
import akka.actor.{Actor, PoisonPill, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods.GET
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import akka.util.ByteString

class PullWorker(feedUrl: String, maxFeedSize: Long) extends Actor {
    import context.dispatcher

    case object Ack
    case object PullFinished

    private val log = org.log4s.getLogger
    private implicit val materializer = ActorMaterializer()
    private val startTime = Instant.now

    override def preStart(): Unit = {
        val url = Uri(feedUrl)
        val request = HttpRequest(GET, url)

        Http(context.system).singleRequest(request) onComplete {
            case Success(response) =>
                self ! response

            case Failure(e) =>
                context.parent ! PullFailure(e.getMessage)
                self ! PoisonPill
        }
    }

    def receive = {
        case response: HttpResponse =>
            log.debug(s"Max feed size: $maxFeedSize bytes")

            response.entity.withSizeLimit(maxFeedSize).dataBytes
                .statefulMapConcat(countBytes(response.entity.contentLengthOption))
                .runWith(Sink.actorRefWithAck(
                    self,
                    PullStart(feedUrl),
                    Ack,
                    PullFinished,
                    (e) => {
                        log.error(e)("Stream error")
                        PullFailure(e.getMessage)
                    }
                ))

            context become downloading
    }

    private def downloading: Receive = {
        case start: PullStart =>
            log.debug("Received import start")
            sender() ! Ack
            context.parent ! start

        case _: ByteString =>
            sender() ! Ack

        case PullFinished =>
            log.debug("Received pull finish")
            context.parent ! PullSummary(Instant.now.getEpochSecond - startTime.getEpochSecond)
            self ! PoisonPill

        case PullFailure(error) =>
            log.error(s"Pull failure: $error")
            self ! PoisonPill
    }

    private def countBytes(maybeSize: Option[Long])() = {
        var bytesSoFar = 0L

        (bytes: ByteString) => {
            bytesSoFar += bytes.length
            context.parent ! PullProgress(bytesSoFar, maybeSize)
            List(bytes)
        }
    }
}

object PullWorker {
    def props(feedUrl: String, maxFeedSize: Long) = Props(new PullWorker(feedUrl, maxFeedSize))
}
