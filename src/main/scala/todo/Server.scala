package todo

import cats.effect.{ConcurrentEffect, Timer}
import org.http4s.HttpRoutes
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import todo.route.Routes
import todo.service.{AuthorizationServiceLive, TodoServiceLive, UserServiceLive}
import zio.{Task, _}

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

object Server {

  trait Service {
    def startServer(implicit cs: ConcurrentEffect[Task], t: Timer[Task]): URIO[Transactional, Unit]
  }

  case class ServerInstance(routes: HttpRoutes[Task]) extends Service {
    override def startServer(implicit cs: ConcurrentEffect[Task], t: Timer[Task]): URIO[Transactional, Unit] = {
      val serverThreadPool = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(2))

      URIO.succeed(
        BlazeServerBuilder
          .apply[Task](serverThreadPool)
          .bindHttp(8080, "0.0.0.0")
          .withHttpApp(routes.orNotFound)
          .resource
          .use(_ => UIO.never)
          .orDie
      )
    }
  }

//  val live :ZLayer[Has[Routes], Nothing, Has[Serv.Service]]= ZLayer.fromService{routes: Routes => Serv2(routes.routes(t))}

}

trait ServerEnvironmentLive extends AuthorizationServiceLive with TodoServiceLive with UserServiceLive
