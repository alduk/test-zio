package zlayers

// UserModel.scala
import zio.{Task, ZIO, ZLayer}

case class User(name: String, email: String)

object UserModel {
  // 1. service
  trait Service {
    def insert(u: User): Task[Unit]
  }

  // 2. layer - service implementation
  val live: ZLayer[DB, Nothing, UserModel] = ZLayer.fromService { db =>
    new Service {
      override def insert(u: User): Task[Unit] =
        db.execute(s"INSERT INTO user VALUES ('${u.name}')")
    }
  }

  // 3. accessor
  def insert(u: User): ZIO[UserModel, Throwable, Unit] = ZIO.accessM(_.get.insert(u))
}


