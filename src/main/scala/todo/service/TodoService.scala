package todo.service

import todo.Endpoints.AuthToken
import todo.LoggingHelper.logMessage
import todo.Models._
import todo.repository.TodoRepository._
import todo.Trx
import todo.service.AuthorizationService.isValidTokenForExistingUserAndExtractUser
import zio.ZIO

object TodoService {

  def handleTodoItemCreation(authToken: AuthToken, createTodo: CreateTodo)(
      implicit transactor:              Trx
  ): ZIO[Any, Throwable, Either[UnauthorizedErrorResponse, EmptyResponse]] = {
    isValidTokenForExistingUserAndExtractUser(authToken)
      .flatMap {
        case None => createUnauthorizedErrorResponse("Unauthorized to create this TODO item")
        case Some(loggedInUser) =>
          saveTodoItem(createTodo, loggedInUser)
            .as[Either[UnauthorizedErrorResponse, EmptyResponse]](Right(EmptyResponse()))
      }
  }

  def handleToDoItemFinish(authToken: AuthToken, id: Int)(
      implicit transactor:            Trx
  ): ZIO[Any, Throwable, Either[ErrorResp, EmptyResponse]] = {

    isValidTokenForExistingUserAndExtractUser(authToken)
      .flatMap {
        case None => createUnauthorizedErrorResponse("Unauthorized to finish this TODO item")
        case Some(loggedInUser) =>
          getOwnerOfTheTodoItem(id)
            .flatMap {
              case None =>
                logMessage(s"No owner found for the todo item with id ${id}")
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
        logMessage(s"Inconsistent data found while marking the item ${id} as done")
          .as(Left(ErrorResponse("Ooops, something went wrong...")))
    }
  }

  private def createForbiddenToFinishItemErrorResponse = {
    Left(ForbiddenResponse("Unauthorized to finish this TODO item"))
  }

  def handleListTodoItems(
      authToken:         AuthToken
  )(implicit transactor: Trx): ZIO[Any, Throwable, Either[ErrorResp, List[Todo]]] = {
    isValidTokenForExistingUserAndExtractUser(authToken)
      .flatMap {
        case None               => createUnauthorizedErrorResponse("Unauthorized to see the todo items")
        case Some(loggedInUser) => listTodoItems(loggedInUser).map[Either[ErrorResp, List[Todo]]](Right(_))
      }
  }

  private def createUnauthorizedErrorResponse(message: String) = {
    ZIO.succeed(Left(UnauthorizedErrorResponse(message)))
  }
}
