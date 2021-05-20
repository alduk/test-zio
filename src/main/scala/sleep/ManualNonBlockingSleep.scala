package sleep

import sleep.ManualNonBlockingSleep.Task.{AndThen, Print, Sleep, Task}

import java.util.concurrent.{ScheduledExecutorService, ScheduledThreadPoolExecutor, TimeUnit}
import scala.collection.mutable

object ManualNonBlockingSleep extends App {

  object Task {

    sealed trait Task {
      self =>
      def andThen(other: Task) = AndThen(self, other)

      def sleep(millis: Long) = Sleep(self, millis)
    }

    case class AndThen(t1: Task, t2: Task) extends Task

    case class Print(id: Int, value: String) extends Task

    case class Sleep(t1: Task, millis: Long) extends Task

  }

  def interpret(task: Task, executor: ScheduledExecutorService): Unit = {
    def loop(current: Task, stack: mutable.Stack[Task]): Unit =
      current match {
        case AndThen(t1, t2) =>
          loop(t1, stack.push(t2))
        case Print(id, value) =>
          stack.headOption match {
            case Some(_) =>
              val v = stack.pop()
              executor.execute(() => {
                println(s"${Thread.currentThread().getName()} $value-$id")
              })
              loop(v, stack)
            case None =>
              executor.execute(() => {
                println(s"${Thread.currentThread().getName()} $value-$id")
              })
          }
        case Sleep(t1, millis) =>
          val r: Runnable = () => {
            loop(t1, stack)
          }
          executor.schedule(r, millis, TimeUnit.MILLISECONDS)
      }

    loop(task, mutable.Stack.empty)
  }

  def nonBlockingFunctionalTask(id: Int) = {
    Print(id, "start") andThen
      Print(id, "middle").sleep(1000) andThen
      Print(id, "end").sleep(1000)
  }

  val scheduler = new ScheduledThreadPoolExecutor(1)
  (1 to 2).toList.map(i => interpret(nonBlockingFunctionalTask(i), scheduler))
  Thread.sleep(4000)
  scheduler.shutdown()
}
