package todo

import cats.implicits._
import org.http4s.HttpRoutes
import sttp.tapir.server.http4s._

import todo.Endpoints._
import todo.TodoService._
import todo.UserService._

import zio._
import zio.interop.catz._
import zio.ZIO

object Routes {

  def createHttp4sRoutes: URIO[Transactional, HttpRoutes[Task]] = {
    ZIO.service[Trx].map { implicit transactor =>
      val listRoute = listTodos.toRoutes {handleListTodoItems(_)}

      val createRoute = createTodo.toRoutes {
        case (authToken, createTodo) => handleTodoItemCreation(authToken, createTodo)
      }

      val finishRoute = finishTodo.toRoutes {
        case (id, authToken) => handleToDoItemFinish(authToken, id)
      }

      val userCreationRoute = createUser.toRoutes(handleUserCreation(_))

      val userAuthenticationRoute = authenticateUser.toRoutes {handleUserAuthentication(_)}

      listRoute <+> createRoute <+> finishRoute <+> userCreationRoute  <+> userAuthenticationRoute
    }
  }
}
