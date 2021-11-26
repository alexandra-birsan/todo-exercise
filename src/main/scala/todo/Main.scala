package todo

import doobie.Transactor
import todo.route.Routes
import todo.util.InitialDatabaseSetup
import zio.{ZLayer, _}
import zio.interop.catz._
import zio.interop.catz.implicits._

object Main extends zio.App with ServerEnvironmentLive {

  def run(args: List[String]): URIO[Any, ExitCode] = {
    val transactor = Transactor.fromDriverManager[Task](
      driver = "org.sqlite.JDBC",
      url    = "jdbc:sqlite:todo.db"
    )

    program
      .provideLayer(ZLayer.succeed(transactor))
      .catchAll(_ => ZIO.succeed().map(_ => ExitCode.failure))
  }

  private val program: ZIO[Transactional, Any, ExitCode] = {
    for {
      _ <- InitialDatabaseSetup.run
      transactor <- ZIO.service[Trx]
      _ <- Task.concurrentEffectWith { implicit ce =>
        Server
          .startServer(transactor)
          .provideLayer(
            (ZLayer.succeed(todoService) ++ ZLayer.succeed(userService)) >>> Routes.live >>> Server.live ++ ZLayer
              .succeed(transactor)
          )
      }
    } yield ExitCode.success
  }
}
