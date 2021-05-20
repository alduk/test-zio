import zio.{ExitCode, Has, Layer, URIO, ZIO, ZLayer}

import java.util.concurrent.Executor

object TestLayer extends zio.App{

  class MyService(e: Executor)

  def mkService(e: Executor) = new MyService(e)

  type MyEnv = Has[MyService]
  val  resources: Layer[Nothing, MyEnv] = {
    val zEx: ZIO[Any, Nothing, Executor] = ZIO.executor.map(_.asJava)
    val zl: ZLayer[Any, Nothing, Has[Executor]] = ZLayer.fromEffect(zEx)
    val zReq: ZLayer[Has[Executor], Nothing, Has[Executor]] = ZLayer.requires[Has[java.util.concurrent.Executor]]
    val zl2: ZLayer[Has[Executor], Nothing, Has[MyService]] = zReq.map(e => Has(mkService(e.get)))
    val comp: ZLayer[Any, Nothing, Has[MyService]] = zl >>> zl2
    comp
  }

  val  resources2: Layer[Nothing, MyEnv] = {
    ZLayer.fromEffect(ZIO.executor.map(_.asJava)) >>>
      ZLayer.fromService((e: java.util.concurrent.Executor) => mkService(e))
  }

  val  resources3: Layer[Nothing, MyEnv] =
    ZLayer.fromEffect(ZIO.executor.map(_.asJava)) >>>
      ZLayer.fromService(mkService)

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = program.provideCustomLayer(resources).exitCode

  val program: ZIO[MyEnv, Nothing, Unit] = ???
}
