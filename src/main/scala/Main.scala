import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory

object Main {
    def main(args: Array[String]): Unit = {
        val config = ConfigFactory.load()
        val system = ActorSystem("downloader")
        system.actorOf(PullSupervisor.props(config, "http://localhost:8000/random_1m.csv"))
    }
}
