package stm

import zio._
import zio.stm._

object TRefTest extends App {
  val createTRef: STM[Nothing, TRef[Int]] = TRef.make(10)
  val commitTRef: UIO[TRef[Int]] = TRef.makeCommit(10)

  val retrieveSingle: UIO[Int] = (for {
    tRef <- TRef.make(10)
    value <- tRef.get
  } yield value).commit

  val updateSingle: UIO[Int] = (for {
    tRef <- TRef.make(10)
    nValue <- tRef.updateAndGet(_ + 20)
  } yield nValue).commit

  val modifySingle: UIO[(String, Int)] = (for {
    tRef <- TRef.make(10)
    mValue <- tRef.modify(v => ("Zee-Oh", v + 10))
    nValue <- tRef.get
  } yield (mValue, nValue)).commit

  val modifyMultiple: UIO[(String, Int)] = for {
    tRef <- TRef.makeCommit(10)
    tuple2 <- tRef.modify(v => ("Zee-Oh", v + 10)).zip(tRef.get).commit
  } yield tuple2

  //val c: STM[Nothing, Int] = ???
  def transfer(tSender: TRef[Int],
               tReceiver: TRef[Int],
               amount: Int): UIO[Int] = {
    STM.atomically {
      val zstm: STM[Nothing, Int] =for {
        _ <- tSender.get.retryUntil(_ >= amount)
        _ <- tSender.update(_ - amount)
        nAmount <- tReceiver.updateAndGet(_ + amount)
      } yield nAmount
      zstm
    }
  }

  val transferredMoney: UIO[String] = for {
    tSender <- TRef.makeCommit(50)
    tReceiver <- TRef.makeCommit(100)
    _ <- transfer(tSender, tReceiver, 50).fork
    _ <- tSender.get.retryUntil(_ == 0).commit
    tuple2 <- tSender.get.zip(tReceiver.get).commit
    (senderBalance, receiverBalance) = tuple2
  } yield s"sender: $senderBalance & receiver: $receiverBalance"

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    val s:UIO[TRef[Int]] = createTRef.commit
    transferredMoney.map(println).exitCode
  }
}
