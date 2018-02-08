import scala.util.{Failure, Success}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.ActorSystem
import akka.pattern.ask
import akka.Done
import akka.util.Timeout
import com.typesafe.config.ConfigFactory

object Main {
    private val log = org.log4s.getLogger

    def main(args: Array[String]): Unit = {
        val config = ConfigFactory.load()
        val system = ActorSystem("downloader")
        val downloader = system.actorOf(PullSupervisor.props(config, system, "http://localhost:8000/random_1m.csv"))

        implicit val timeout = Timeout(5.minutes)

        (downloader ? PullSupervisor.StartPull).mapTo[Done] onComplete {
            case Success(Done) =>
                log.info("Completed successfully")
                system.terminate()

            case Failure(e) =>
                log.error(e)("Failed")
                system.terminate()
        }
    }
}
