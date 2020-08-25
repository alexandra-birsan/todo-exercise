package todo

import todo.Models.{AuthenticationResponse, EmptyResponse, ErrorResponse, Todo, User}
import zio.{Task, ZIO}
import doobie.implicits._
import org.sqlite.SQLiteErrorCode
import todo.Authorization.generateToken
import todo.LoggingHelper.logErrorMessage
import todo.Models._
import todo.PasswordUtils.determineHash
import todo.repository.UserRepository._
import zio.interop.catz._

object UserService {

  def handleUserCreation(createUser: UserCreation)
                        (implicit transactor: Trx): ZIO[Any, Throwable, Either[ErrorResponse, EmptyResponse]] = {
    val passwordHash = determineHash(createUser.password)
    saveUser(createUser, passwordHash)
      .flatMap {
        case Left(e) if (e.getErrorCode == SQLiteErrorCode.SQLITE_CONSTRAINT.code) =>
          ZIO.effect(println(e)).as(Left(ErrorResponse("User already exists")))
        case Left(_) => ZIO.succeed(Left(ErrorResponse("Error while creating the user")))
        case _ => ZIO.succeed(Right(EmptyResponse()))
      }
  }

  def handleUserAuthentication(credentials: UserCreation)
                              (implicit transactor: Trx): ZIO[Any, Throwable, Either[UnAuthorizedErrorResponse, AuthenticationResponse]] = {
    checkCredentials(credentials.name, determineHash(credentials.password))
      .transact(transactor)
      .tapError(logErrorMessage)
      .flatMap {
        case 1 => ZIO.succeed(Right(AuthenticationResponse(generateToken(credentials.name))))
        case _ => ZIO.succeed(Left(UnAuthorizedErrorResponse("Authentication failed")))
      }
  }

  def checkUsernameExists(name: String)(implicit transactor: Trx): Task[Boolean] = {
    getFirstUserWithName(name).map(_.isDefined)
  }
}
