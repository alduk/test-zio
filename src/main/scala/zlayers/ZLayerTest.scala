package zlayers

import zio._
/*
Managing dependencies using ZIO
https://blog.softwaremill.com/managing-dependencies-using-zio-8acc1539e276
 */
object ZLayerTest extends App{

  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, ExitCode] = {
    // using the UserRegistration's method accessor to construct the program,
    // outside of a layer definition
    val program: ZIO[UserRegistration, Throwable, User] =
    UserRegistration.register(User("adam", "adam@hello.world"))

    // composing layers to create a DB instance
    val dbLayer: ZLayer[Any, Throwable, DB] =
      ZLayer.succeed(DBConfig("jdbc://localhost")) >>>
        ConnectionPoolIntegration.live >>>
        DB.liveRelationalDB

    // composing layers to create a UserRegistration instance
    val userRegistrationLayer: ZLayer[Any, Throwable, UserRegistration] =
      ((dbLayer >>> UserModel.live) ++ UserNotifier.live) >>> UserRegistration.live

    // creating the complete application description
    val withLayer = program.provideLayer(userRegistrationLayer)
    program
      .provideLayer(userRegistrationLayer)
      .catchAll(t => ZIO.succeed(t.printStackTrace()).map(_ => ExitCode.failure))
      .map { u =>
        println(s"Registered user: $u (layers)")
        ExitCode.success
      }
  }
}
