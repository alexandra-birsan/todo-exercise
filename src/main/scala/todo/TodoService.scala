package todo

import todo.Endpoints.AuthToken
import todo.Models.{CreateTodo, EmptyResponse, ErrorResp, ErrorResponse, ForbiddenResponse, Todo, UnAuthorizedErrorResponse}
import zio.ZIO
import doobie.implicits._
import cats.implicits._
import cats.effect.{ConcurrentEffect, Timer}
import org.http4s.implicits._
import org.http4s.HttpRoutes
import org.http4s.server.blaze.BlazeServerBuilder
import sttp.tapir._
import sttp.tapir.json.circe._
import sttp.tapir.server.http4s._
import todo.repository.TodoRepository
import todo.repository.TodoRepository._
import zio._
import zio.interop.catz._
object TodoService {

  def handleTodoItemCreation(authToken: AuthToken, createTodo: CreateTodo)
                            (implicit transactor: Trx): ZIO[Any, Throwable, Either[UnAuthorizedErrorResponse, EmptyResponse]] = {
    Authorization
      .isValidTokenForExistingUserAndExtractUser(authToken)
      .flatMap(
        loggedInUser =>
          if (loggedInUser.isDefined) {
            TodoRepository.saveTodoItem(createTodo, loggedInUser)
              .as[Either[UnAuthorizedErrorResponse, EmptyResponse]](Right(EmptyResponse()))
          } else ZIO.succeed(Left(UnAuthorizedErrorResponse("Unauthorized to create this TODO item")))
      )
  }

  def handleToDoItemFinish(authToken: AuthToken, id: Int)
                          (implicit transactor: Trx): ZIO[Any, Throwable, Either[ErrorResp, EmptyResponse]] = {
    Authorization
      .isValidTokenForExistingUserAndExtractUser(authToken)
      .flatMap(
        username =>
          if (username.isEmpty)
            ZIO.succeed(Left(UnAuthorizedErrorResponse("Unauthorized to finish this TODO item")))
          else {
            InitialDatabaseSetup
              .getOwnerOfTheTodoItem(id)
              .flatMap(
                owner =>
                  if (owner.isDefined) {
                    if (owner.get == username.get) {
                      finishTodoItem(id)
                        .flatMap {
                          //                                case 0 => ZIO.succeed(Left(ErrorResponse(s"Todo with id: `${id}` not found")))
                          case 1 => ZIO.succeed(Right[ErrorResp, EmptyResponse](EmptyResponse()))
                          case _ =>
                            ZIO
                              .effect(
                                println(s"Inconsistent data: More than one todo updated in POST /todo/${id}")
                              )
                              .as(Left(ErrorResponse("Ooops, something went wrong...")))
                        }
                    } else
                      ZIO.effect(println(s"The logged in user is not the owner of the item with id ${id}"))
                        .as(Left(ForbiddenResponse("Unauthorized to finish this TODO item")))
                  } else ZIO.effect("No owner found for the to do item")
                    .as(Left(ForbiddenResponse("Unauthorized to finish this TODO item")))
              )
          }
      )
  }

  def handleListTodoItems(authToken: AuthToken)
                         (implicit transactor:Trx): ZIO[Any, Throwable, Either[ErrorResp, List[Todo]]] = {
      Authorization
        .isValidTokenForExistingUserAndExtractUser(authToken)
        .flatMap(
          loggedInUser =>
            if (loggedInUser.isEmpty)
              ZIO.succeed(Left(UnAuthorizedErrorResponse("Unauthorized to access this endpoint")))
            else {
              listTodoItems( loggedInUser)
                .map[Either[ErrorResp, List[Todo]]](Right(_))
            }
        )
  }
}
