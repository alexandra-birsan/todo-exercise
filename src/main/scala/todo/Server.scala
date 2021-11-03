package todo

import cats.effect.{ConcurrentEffect, Timer}
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import todo.route.Routes
import todo.service.{
  AuthorizationService,
  AuthorizationServiceLive,
  TodoService,
  TodoServiceLive,
  UserService,
  UserServiceLive
}
import zio._

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

trait Server extends Routes with ServerEnvironment {

  def startServer(implicit cs: ConcurrentEffect[Task], t: Timer[Task]): URIO[Transactional, Unit] = {
    val serverThreadPool = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(2))

    createHttp4sRoutes().flatMap { routes =>
      BlazeServerBuilder
        .apply[Task](serverThreadPool)
        .bindHttp(8080, "0.0.0.0")
        .withHttpApp(routes.orNotFound)
        .resource
        .use(_ => UIO.never)
        .orDie
    }
  }
}

trait ServerEnvironment extends AuthorizationService with TodoService with UserService

trait ServerEnvironmentLive
    extends ServerEnvironment
    with AuthorizationServiceLive
    with TodoServiceLive
    with UserServiceLive
