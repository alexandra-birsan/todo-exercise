package todo

import doobie.Transactor
import org.http4s.implicits.http4sKleisliResponseSyntaxOptionT
import org.http4s.server.blaze.BlazeServerBuilder
import todo.Server.ServerInstance
import todo.route.Routes
import todo.service.{TodoService, UserService}
import todo.util.InitialDatabaseSetup
import zio._
import zio.interop.catz._
import zio.interop.catz.implicits._

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

object Main extends zio.App with ServerEnvironmentLive {

  def run(args: List[String]): URIO[ZEnv, ExitCode] = {
    val transactor = Transactor.fromDriverManager[Task](
      driver = "org.sqlite.JDBC",
      url    = "jdbc:sqlite:todo.db"
    )

    program.provideLayer(ZLayer.succeed(transactor))
  }

  private val _userService = userService
  private val _todoService = todoService

  private val routesInstance = new Routes {
    override val userService: UserService.Service = _userService
    override val todoService: TodoService.Service = _todoService
  }

  private val program: URIO[Transactional, ExitCode] = {
    for {
      _ <- InitialDatabaseSetup.run
      transactor <- ZIO.service[Trx]
      _ <- Task.concurrentEffectWith { implicit ce =>
        val routes = routesInstance.routes(transactor)
        ServerInstance(routes).startServer.provide(Has(transactor))
      }
    } yield ExitCode.success
  }
}
