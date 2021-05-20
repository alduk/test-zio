package http

import zio._
import zhttp.http._
import zhttp.service.Server
import zio.logging._

object HelloWorld extends App {

  val env =
    Logging.console(
      logLevel = LogLevel.Info,
      format = LogFormat.ColoredLogFormat()
    ) >>> Logging.withRootLoggerName("HelloWorld")

  val app: Http[Logging, HttpError, Request, Response] = Http.collectM[Request] {
    case Method.GET -> Root / "text" =>
      log.info("Processing /text") *>
      ZIO.succeed(Response.text("Hello World!")) <* log.info("Processing end /text")
  }

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    Server.start(8090, app).provideLayer(env).exitCode
}
