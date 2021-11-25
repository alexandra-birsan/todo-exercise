package todo

import cats.effect.{ConcurrentEffect, Timer}
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import todo.route.Routes
import todo.service.{AuthorizationServiceLive, TodoServiceLive, UserServiceLive}
import zio.{Task, _}

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

object Server {

  // 1- service
  trait Service {
    def startServer(implicit cs: ConcurrentEffect[Task], t: Timer[Task], trx: Trx): URIO[Transactional, Unit]
  }

  // 2. layer -  service implementation
  val live: ZLayer[Has[Routes], Nothing, Has[Server.Service]] = ZLayer.fromService { routes: Routes =>
    new Service {
      override def startServer(
          implicit cs: ConcurrentEffect[Task],
          t:           Timer[Task],
          trx:         Trx
      ): URIO[Transactional, Unit] = {
        val serverThreadPool = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(2))

        URIO.succeed(
          BlazeServerBuilder
            .apply[Task](serverThreadPool)
            .bindHttp(8080, "0.0.0.0")
            .withHttpApp(routes.routes(trx).orNotFound)
            .resource
            .use(_ => UIO.never)
            .orDie
        )
      }
    }
  }

  // 3. accessor
  def startServer(
      trx:       Trx
  )(implicit cs: ConcurrentEffect[Task], t: Timer[Task]): ZIO[Has[Server.Service] with Transactional, Throwable, Unit] =
    ZIO.accessM(_.get.startServer(cs, t, trx))
}

trait ServerEnvironmentLive extends AuthorizationServiceLive with TodoServiceLive with UserServiceLive
