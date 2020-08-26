package todo.service

import org.sqlite.SQLiteErrorCode
import AuthorizationService.generateToken
import todo.Models._
import todo.PasswordUtils.determineHash
import todo.Trx
import todo.repository.UserRepository.{checkCredentials, getFirstUserWithName, saveUser}
import zio.{Task, ZIO}

object UserService {

  def handleUserCreation(
      createUser:        UserCreation
  )(implicit transactor: Trx): ZIO[Any, Throwable, Either[ErrorResponse, EmptyResponse]] = {
    val passwordHash = determineHash(createUser.password)
    saveUser(createUser, passwordHash)
      .flatMap {
        case Left(e) if (e.getErrorCode == SQLiteErrorCode.SQLITE_CONSTRAINT.code) =>
          ZIO.effect(println(e)).as(Left(ErrorResponse("User already exists")))
        case Left(_) => ZIO.succeed(Left(ErrorResponse("Error while creating the user")))
        case _       => ZIO.succeed(Right(EmptyResponse()))
      }
  }

  def handleUserAuthentication(
      credentials:       UserCreation
  )(implicit transactor: Trx): ZIO[Any, Throwable, Either[UnauthorizedErrorResponse, AuthenticationResponse]] = {
    checkCredentials(credentials.name, determineHash(credentials.password))
      .flatMap {
        case Some(_) => ZIO.succeed(Right(AuthenticationResponse(generateToken(credentials.name))))
        case None => ZIO.succeed(Left(UnauthorizedErrorResponse("Authentication failed")))
      }
  }

  def checkUsernameExists(name: String)(implicit transactor: Trx): Task[Boolean] = {
    getFirstUserWithName(name).map(_.isDefined)
  }
}
