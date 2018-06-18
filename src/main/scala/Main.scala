import akka.Done
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.common.{CsvEntityStreamingSupport, EntityStreamingSupport}
import akka.http.scaladsl.marshalling.{Marshaller, Marshalling}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.stream.scaladsl.Source
import akka.stream.{ActorMaterializer, Materializer}
import akka.util.{ByteString, Timeout}
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object Main {
  private val log = org.log4s.getLogger

  private def server(): Future[Http.ServerBinding] = {
    implicit val system: ActorSystem = ActorSystem("server")
    implicit val materializer: Materializer = ActorMaterializer()
    implicit val marshaller: Marshaller[String, ByteString] =
      Marshaller.strict[String, ByteString] { s =>
        Marshalling.WithFixedContentType(ContentTypes.`text/csv(UTF-8)`, () => {
          ByteString(s"$s," * 100 + "\r\n")
        })
      }
    implicit val csvEntityStreamingSupport: CsvEntityStreamingSupport = EntityStreamingSupport.csv()

    val route = path(Remaining) { p =>
      complete(
        Source.repeat(p).take(1000000)
      )
    }

    Http().bindAndHandle(route, "localhost", 8000)
  }

  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load()
    val system = ActorSystem("downloader")

    server().foreach(binding => {

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
    })
  }
}
