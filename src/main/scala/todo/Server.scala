package todo

import cats.effect.{ConcurrentEffect, Timer}
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import todo.route.Routes
import zio._

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

object Server extends Routes {

  def run(implicit cs: ConcurrentEffect[Task], t: Timer[Task]): URIO[Transactional, Unit] = {
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
