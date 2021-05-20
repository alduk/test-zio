package sleep

import zio._
import zio.console._
import zio.duration._

object TestZioSleep extends zio.App {
  def zioTask(id: Int) =
    for {
      _ <- putStrLn(s"${Thread.currentThread().getName()} start-$id")
      _ <- ZIO.sleep(10.seconds)
      _ <- putStrLn(s"${Thread.currentThread().getName()} end-$id")
    } yield id

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    ZIO.foreachPar(((1 to 10)))(i => zioTask(i)).exitCode
    //zioTask(1).exitCode
  }
}
