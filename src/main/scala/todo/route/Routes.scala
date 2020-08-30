package todo.route

import cats.implicits._
import org.http4s.HttpRoutes
import sttp.tapir.server.http4s._
import todo.{Transactional, Trx}
import todo.route.Endpoints._
import todo.service.TodoService._
import todo.service.UserService._
import zio._
import zio.interop.catz._
import zio.ZIO


object Routes {

  def createHttp4sRoutes: URIO[Transactional, HttpRoutes[Task]] = {
    ZIO.service[Trx].map { implicit transactor =>
      routes
    }

  }

   def routes(implicit trx: Trx): HttpRoutes[Task] = {
    val listRoute = listTodos.toRoutes {
      handleListTodoItems(_)
    }

    val createRoute = createTodo.toRoutes {
      case (authToken, createTodo) => handleTodoItemCreation(authToken, createTodo)
    }

    val finishRoute = finishTodo.toRoutes {
      case (authToken, id) => handleToDoItemFinish(authToken, id)
    }

    val userCreationRoute = createUser.toRoutes(handleUserCreation(_))

    val userAuthenticationRoute = authenticateUser.toRoutes {
      handleUserAuthentication(_)
    }

    listRoute <+> createRoute <+> finishRoute <+> userCreationRoute <+> userAuthenticationRoute
  }
}
