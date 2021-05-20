package stm

import zio._
import zio.stm._

object TQueueTest extends App {
  val tQueueBounded: STM[Nothing, TQueue[Int]] = TQueue.bounded[Int](5)

  val tQueueUnbounded: STM[Nothing, TQueue[Int]] = TQueue.unbounded[Int]

  val tQueueOffer: UIO[TQueue[Int]] = (for {
    tQueue <- TQueue.bounded[Int](3)
    _      <- tQueue.offer(1)
  } yield tQueue).commit

  val tQueueOfferAll: UIO[TQueue[Int]] = (for {
    tQueue <- TQueue.bounded[Int](3)
    _      <- tQueue.offerAll(List(1, 2))
  } yield tQueue).commit

  val tQueueTake: UIO[Int] = (for {
    tQueue <- TQueue.bounded[Int](3)
    _      <- tQueue.offerAll(List(1, 2))
    res    <- tQueue.take
  } yield res).commit

  val tQueueTakeUpTo: UIO[List[Int]] = (for {
    tQueue <- TQueue.bounded[Int](4)
    _      <- tQueue.offerAll(List(1, 2))
    res    <- tQueue.takeUpTo(3)
  } yield res).commit

  val tQueueTakeAll: UIO[List[Int]] = (for {
    tQueue <- TQueue.bounded[Int](4)
    _      <- tQueue.offerAll(List(1, 2))
    res    <- tQueue.takeAll
  } yield res).commit


  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = tQueueTakeAll.map(println).exitCode
}
