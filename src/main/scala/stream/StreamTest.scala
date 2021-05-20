package stream

import zio._
import zio.clock.Clock
import zio.console.{Console, putStrLn}
import zio.duration.durationInt
import zio.stream.ZTransducer.{deflate, gunzip, inflate}
import zio.stream._
import zio.stream.compression.{CompressionException, CompressionLevel, CompressionStrategy, FlushMode}

object StreamTest extends App {
  val stream: Stream[Nothing, Int] = Stream(1,2,3)

  val streamFromIterable: Stream[Nothing, Int] = Stream.fromIterable(0 to 100)

  val intStream: Stream[Nothing, Int] = Stream.fromIterable(0 to 100)
  val stringStream: Stream[Nothing, String] = intStream.map(_.toString)

  val partitionResult: ZManaged[Any, Nothing, (ZStream[Any, Nothing, Int], ZStream[Any, Nothing, Int])] =
    Stream
      .fromIterable(0 to 100)
      .partition(_ % 2 == 0, buffer = 50)

  val groupedResult: ZStream[Any, Nothing, Chunk[Int]] =
    Stream
      .fromIterable(0 to 100)
      .grouped(50)


  case class Exam(person: String, score: Int)

  val examResults = Seq(
    Exam("Alex", 64),
    Exam("Michael", 97),
    Exam("Bill", 77),
    Exam("John", 78),
    Exam("Bobby", 71)
  )

  val groupByKeyResult: ZStream[Any, Nothing, (Int, Int)] =
    Stream
      .fromIterable(examResults)
      .groupByKey(exam => exam.score / 10 * 10) {
        case (k, s) => ZStream.fromEffect(s.runCollect.map(l => k -> l.size))
      }

  val groupedWithinResult: ZStream[Any with Clock, Nothing, Chunk[Int]] =
    Stream.fromIterable(0 to 10)
      .repeat(Schedule.spaced(1.seconds))
      .groupedWithin(30, 10.seconds)

  val result: RIO[Console, Unit] = Stream.fromIterable(0 to 100).foreach(i => putStrLn(i.toString))

  def streamReduce(total: Int, element: Int): Int = total + element
  val resultFromSink: UIO[Int] = Stream(1,2,3).run(Sink.foldLeft(0)(streamReduce))

  val merged: Stream[Nothing, Int] = Stream(1,2,3).merge(Stream(2,3,4))

  val zippedStream: Stream[Nothing, (Int, Int)] = Stream(1,2,3).zip(Stream(2,3,4))

  def tupleStreamReduce(total: Int, element: (Int, Int)) = {
    val (a,b) = element
    total + (a + b)
  }

  val reducedResult: UIO[Int] = zippedStream.run(Sink.foldLeft(0)(tupleStreamReduce))

  def decompressDeflated(deflated: ZStream[Any, Nothing, Byte]): ZStream[Any, CompressionException, Byte] = {
    val bufferSize: Int = 64 * 1024 // Internal buffer size. Few times bigger than upstream chunks should work well.
    val noWrap: Boolean = false     // For HTTP Content-Encoding should be false.
    deflated.transduce(inflate(bufferSize, noWrap))
  }

  def decompressGzipped(gzipped: ZStream[Any, Nothing, Byte]): ZStream[Any, CompressionException, Byte] = {
    val bufferSize: Int = 64 * 1024 // Internal buffer size. Few times bigger than upstream chunks should work well.
    gzipped.transduce(gunzip(bufferSize))
  }

  def compressWithDeflate(clearText: ZStream[Any, Nothing, Byte]): ZStream[Any, Nothing, Byte] = {
    val bufferSize: Int = 64 * 1024 // Internal buffer size. Few times bigger than upstream chunks should work well.
    val noWrap: Boolean = false // For HTTP Content-Encoding should be false.
    val level: CompressionLevel = CompressionLevel.DefaultCompression
    val strategy: CompressionStrategy = CompressionStrategy.DefaultStrategy
    val flushMode: FlushMode = FlushMode.NoFlush
    clearText.transduce(deflate(bufferSize, noWrap, level, strategy, flushMode))
  }

  def deflateWithDefaultParameters(clearText: ZStream[Any, Nothing, Byte]): ZStream[Any, Nothing, Byte] =
    clearText.transduce(deflate())

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    zippedStream.foreach(i => putStrLn(i.toString)).exitCode
}
