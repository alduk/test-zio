package managed
import zio._
import zio.console.Console

import java.io.{File, IOException}

object ManagedTest extends App {

  def doSomething(queue: Queue[Int]): UIO[Unit] = IO.unit
  val managedResource: Managed[Nothing, Queue[Int]] = Managed.make(Queue.unbounded[Int])(_.shutdown)
  val usedResource: UIO[Unit] = managedResource.use { queue => doSomething(queue) }

  def acquire: IO[Throwable, Int] = IO.effect(???)

  val managedFromEffect: Managed[Throwable, Int] = Managed.fromEffect(acquire)

  val managedFromValue: Managed[Nothing, Int] = Managed.succeed(3)

  val zManagedResource: ZManaged[Console, Nothing, Unit] =
    ZManaged.make(console.putStrLn("acquiring"))(_ => console.putStrLn("releasing"))
  val zUsedResource: URIO[Console, Unit] = zManagedResource.use { _ => console.putStrLn("running") }

  val managedQueue: Managed[Nothing, Queue[Int]] = Managed.make(Queue.unbounded[Int])(_.shutdown)
  val managedFile: ZManaged[Console, Throwable, File] = Managed.make(ZIO.effect(new File("data.json")))(f => console.putStrLn(s"closing $f"))

  val combined: ZManaged[Console, Throwable, (Queue[Int], File)] = for {
    queue <- managedQueue
    file  <- managedFile
  } yield (queue, file)

  def doSomething2(queue: Queue[Int], file: File): URIO[Console, Unit] = console.putStrLn(s"processing $file with $queue")
  val usedCombinedRes: ZIO[Console, Throwable, Unit] = combined.use { case (queue, file) => doSomething2(queue, file) }

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = usedCombinedRes.exitCode
}
