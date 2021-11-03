package todo.service

import todo.route.Endpoints.AuthToken
import todo.util.LoggingHelper.logMessage
import todo.model.Models._
import todo.repository.TodoRepository._
import todo.Trx
import zio.ZIO

object TodoService {

  trait Service {
    def handleTodoItemCreation(authToken: AuthToken, createTodo: CreateTodo)(
        implicit transactor:              Trx
    ): ZIO[Any, Throwable, Either[UnauthorizedErrorResponse, EmptyResponse]]

    def handleToDoItemFinish(authToken: AuthToken, id: Int)(
        implicit transactor:            Trx
    ): ZIO[Any, Throwable, Either[ErrorResp, EmptyResponse]]

    def handleListTodoItems(
        authToken:         AuthToken
    )(implicit transactor: Trx): ZIO[Any, Throwable, Either[ErrorResp, List[Todo]]]
  }
}

trait TodoService {

  val todoService: TodoService.Service
}

trait TodoServiceLive extends TodoService with AuthorizationService {

  override val todoService: TodoService.Service = new TodoService.Service {

    override def handleTodoItemCreation(authToken: AuthToken, createTodo: CreateTodo)(
        implicit transactor:                       Trx
    ): ZIO[Any, Throwable, Either[UnauthorizedErrorResponse, EmptyResponse]] = {
      authorizationService
        .isValidTokenForExistingUserAndExtractUser(authToken)
        .flatMap {
          case None => createUnauthorizedErrorResponse("Unauthorized to create this TODO item")
          case Some(loggedInUser) =>
            saveTodoItem(createTodo, loggedInUser)
              .as[Either[UnauthorizedErrorResponse, EmptyResponse]](Right(EmptyResponse()))
        }
    }

    override def handleToDoItemFinish(authToken: AuthToken, id: Int)(
        implicit transactor:                     Trx
    ): ZIO[Any, Throwable, Either[ErrorResp, EmptyResponse]] = {

      authorizationService
        .isValidTokenForExistingUserAndExtractUser(authToken)
        .flatMap {
          case None => createUnauthorizedErrorResponse("Unauthorized to finish this TODO item")
          case Some(loggedInUser) =>
            getOwnerOfTheTodoItem(id)
              .flatMap {
                case None =>
                  logMessage(s"No owner found for the todo item with id $id")
                    .as(createForbiddenToFinishItemErrorResponse)
                case Some(owner) if owner == loggedInUser => markTodoAsDone(id)
                case _ =>
                  logMessage(s"The logged in user is not the owner of the item with id $id")
                    .as(createForbiddenToFinishItemErrorResponse)
              }
        }
    }

    private def markTodoAsDone(
        id:                Int
    )(implicit transactor: Trx): ZIO[Any, Throwable, Either[ErrorResp, EmptyResponse]] = {
      finishTodoItem(id).flatMap {
        case 1 => ZIO.succeed(Right[ErrorResp, EmptyResponse](EmptyResponse()))
        case _ =>
          logMessage(s"Inconsistent data found while marking the item $id as done")
            .as(Left(ErrorResponse("Ooops, something went wrong...")))
      }
    }

    private def createForbiddenToFinishItemErrorResponse = {
      Left(ForbiddenResponse("Unauthorized to finish this TODO item"))
    }

    override def handleListTodoItems(
        authToken:         AuthToken
    )(implicit transactor: Trx): ZIO[Any, Throwable, Either[ErrorResp, List[Todo]]] = {
      authorizationService
        .isValidTokenForExistingUserAndExtractUser(authToken)
        .flatMap {
          case None               => createUnauthorizedErrorResponse("Unauthorized to see the todo items")
          case Some(loggedInUser) => listTodoItems(loggedInUser).map[Either[ErrorResp, List[Todo]]](Right(_))
        }
    }

    private def createUnauthorizedErrorResponse(message: String) = {
      ZIO.succeed(Left(UnauthorizedErrorResponse(message)))
    }
  }
}
