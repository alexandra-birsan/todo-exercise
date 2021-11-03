package todo.route

import cats.implicits._
import org.http4s.HttpRoutes
import sttp.tapir.server.http4s._
import todo.route.Endpoints._
import todo.service.{TodoService, UserService}
import todo.{Transactional, Trx}
import zio._
import zio.interop.catz._

trait Routes extends TodoService with UserService {

  def createHttp4sRoutes(): URIO[Transactional, HttpRoutes[Task]] = {
    ZIO.service[Trx].map { implicit transactor =>
      routes
    }
  }

  def routes(implicit trx: Trx): HttpRoutes[Task] = {
    val listRoute = listTodos.toRoutes {
      todoService.handleListTodoItems(_)
    }

    val createRoute = createTodo.toRoutes {
      case (authToken, createTodo) => todoService.handleTodoItemCreation(authToken, createTodo)
    }

    val finishRoute = finishTodo.toRoutes {
      case (authToken, id) => todoService.handleToDoItemFinish(authToken, id)
    }

    val userCreationRoute = createUser.toRoutes(userService.handleUserCreation(_))

    val userAuthenticationRoute = authenticateUser.toRoutes {
      userService.handleUserAuthentication(_)
    }

    listRoute <+> createRoute <+> finishRoute <+> userCreationRoute <+> userAuthenticationRoute
  }
}
