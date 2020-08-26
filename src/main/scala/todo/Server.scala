package todo

import java.util.concurrent.Executors

import cats.effect.{ConcurrentEffect, Timer}
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import todo.route.Routes

import scala.concurrent.ExecutionContext
import zio._

object Server {

  def run(implicit cs: ConcurrentEffect[Task], t: Timer[Task]): URIO[Transactional, Unit] = {
    val serverThreadPool = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(2))

    Routes.createHttp4sRoutes.flatMap { routes =>
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
