package todo

import doobie.Transactor
import todo.route.Routes
import todo.service.{TodoService, UserService}
import todo.util.InitialDatabaseSetup
import zio._
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

  private val _userService = userService
  private val _todoService = todoService

  private val routesInstance = new Routes {
    override val userService: UserService.Service = _userService
    override val todoService: TodoService.Service = _todoService
  }

  private val program: ZIO[Transactional, Any, ExitCode] = {
    for {
      _ <- InitialDatabaseSetup.run
      transactor <- ZIO.service[Trx]
      _ <- Task.concurrentEffectWith { implicit ce =>
        Server
          .startServer(transactor)
          .provideLayer(Server.live ++ ZLayer.succeed(routesInstance) ++ ZLayer.succeed(transactor))
          .provideLayer(ZLayer.succeed(routesInstance))
      }
    } yield ExitCode.success
  }
}
