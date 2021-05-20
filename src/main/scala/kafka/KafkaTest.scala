package kafka

import zio._
import zio.blocking.Blocking
import zio.clock.Clock
import zio.duration._
import zio.kafka.consumer._
import zio.console._
import zio.kafka.serde._

import java.io.{File, IOException}
import java.net.Socket

/*
UIO[A] — This is a type alias for ZIO[Any, Nothing, A], which represents an effect that has no requirements, and cannot fail, but can succeed with an A.
URIO[R, A] — This is a type alias for ZIO[R, Nothing, A], which represents an effect that requires an R, and cannot fail, but can succeed with an A.
Task[A] — This is a type alias for ZIO[Any, Throwable, A], which represents an effect that has no requirements, and may fail with a Throwable value, or succeed with an A.
RIO[R, A] — This is a type alias for ZIO[R, Throwable, A], which represents an effect that requires an R, and may fail with a Throwable value, or succeed with an A.
IO[E, A] — This is a type alias for ZIO[Any, E, A], which represents an effect that has no requirements, and may fail with an E, or succeed with an A.
 */

object KafkaTest extends App {

  val settings: ConsumerSettings =
    ConsumerSettings(List("localhost:9092"))
      .withGroupId("group")
      .withClientId("client")
      .withCloseTimeout(30.seconds)

  val subscription = Subscription.topics("topic")

  case class User(teamId: String)
  trait Team

  val maybeId: IO[Option[Nothing], String] = ZIO.fromOption(Some("abc123"))
  def getUser(userId: String): IO[Throwable, Option[User]] = ???
  def getTeam(teamId: String): IO[Throwable, Team] = ???

  lazy val result: IO[Throwable, Option[(User, Team)]] = (for {
    id   <- maybeId
    user <- getUser(id).some
    team <- getTeam(user.teamId).asSomeError
  } yield (user, team)).optional

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    val prog: ZIO[Console with Blocking with Clock, Throwable, Unit] = Consumer.consumeWith(settings, subscription, Serde.string, Serde.string) { case (key, value) =>
      putStrLn(s"Received message ${key}: ${value}")
    }
    prog.exitCode
  }

  import scala.io.{ Codec, Source }
  import zio.blocking._

  import java.net.ServerSocket
  import zio.UIO

  def accept(l: ServerSocket): RIO[Blocking, Socket] =
    effectBlockingCancelable(l.accept())(UIO.effectTotal(l.close()))

  def download(url: String): Task[String] =
    Task.effect {
      Source.fromURL(url)(Codec.UTF8).mkString
    }

  def safeDownload(url: String): ZIO[Blocking, Throwable, String] =
    blocking(download(url))

  val zipped: UIO[(String, Int)] =
    ZIO.succeed("4").zip(ZIO.succeed(2))


  val zeither: UIO[Either[String, Int]] =
    IO.fail("Uh oh!").either


  def sqrt(io: UIO[Double]): IO[String, Double] =
    ZIO.absolve(
      io.map(value =>
        if (value < 0.0) Left("Value must be >= 0.0")
        else Right(Math.sqrt(value))
      )
    )

  def openFile(path: String): IO[IOException, File] = ZIO.effect(new File(path)).refineToOrDie

  def closeFile(f: File): IO[IOException, Unit] = ???

  val z: IO[IOException, File] =
    openFile("primary.json").catchAll(_ =>
      openFile("backup.json"))

  val retriedOpenFile: ZIO[Clock, IOException, File] =
    openFile("primary.data").retry(Schedule.recurs(5))


  /*val groupedFileData: URIO[IOException, Unit] =
    openFile("data.json").bracket(closeFile(_)) { file =>
      for {
        data    <- decodeData(file)
        grouped <- groupData(data)
      } yield grouped
    }*/
}
