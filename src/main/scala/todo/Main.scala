package todo

import doobie.Transactor
import route.Routes
import service.{AuthorizationService, TodoService, TokenService, UserService}
import util.InitialDatabaseSetup
import zio.{ZLayer, _}
import zio.interop.catz._
import zio.interop.catz.implicits._

object Main extends zio.App {

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
    val serverLayer = createServerLayer()
    for {
      _ <- InitialDatabaseSetup.run
      transactor <- ZIO.service[Trx]
      _ <- Task.concurrentEffectWith { implicit ce =>
        Server
          .startServer(transactor)
          .provideLayer(
            serverLayer >>> Server.live ++ ZLayer.succeed(transactor)
          )
      }
    } yield ExitCode.success
  }

  private def createServerLayer() = {
    val userServiceLayer: ZLayer[Any, Throwable, Has[UserService.Service]] = TokenService.live >>> UserService.live
    val authorizationServiceLayer: ZLayer[Any, Throwable, Has[AuthorizationService.Service]] =
      userServiceLayer >>> AuthorizationService.live
    val todoServiceLayer
        : ZLayer[Any, Throwable, Has[TodoService.Service]] = authorizationServiceLayer >>> TodoService.live
    val routesServiceLayer
        : ZLayer[Any, Throwable, Has[UserService.Service] with Has[TodoService.Service]] = todoServiceLayer ++ userServiceLayer
    val serverLayer: ZLayer[Any, Throwable, Has[Routes.Service]] = routesServiceLayer >>> Routes.live
    serverLayer
  }
}
