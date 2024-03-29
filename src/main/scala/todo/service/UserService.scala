package todo.service

import org.sqlite.SQLiteErrorCode
import todo.model.Models._
import todo.util.PasswordUtils.determineHash
import todo.Trx
import todo.repository.UserRepository.{checkCredentials, getFirstUserWithName, saveUser}
import zio.{Has, Task, ZIO, ZLayer}

object UserService {

  trait Service {
    def handleUserCreation(
        createUserInfo:    UserCredentials
    )(implicit transactor: Trx): ZIO[Any, Throwable, Either[ErrorResponse, EmptyResponse]]
    def handleUserAuthentication(
        credentials:              UserCredentials
    )(implicit transactor:        Trx): ZIO[Any, Throwable, Either[UnauthorizedErrorResponse, AuthenticationResponse]]
    def checkUsernameExists(name: String)(implicit transactor: Trx): Task[Boolean]
  }

  val live: ZLayer[Has[TokenService.Service], Throwable, Has[UserService.Service]] =
    ZLayer.fromService(
      tokenService =>
        new UserService.Service {

          override def handleUserCreation(
              createUserInfo:    UserCredentials
          )(implicit transactor: Trx): ZIO[Any, Throwable, Either[ErrorResponse, EmptyResponse]] = {
            checkUsernameExists(createUserInfo.name).flatMap {
              if (_)
                ZIO
                  .effect(println(s"User with name ${createUserInfo.name} already exists"))
                  .as(Left(ErrorResponse("User already exists")))
              else createUser(createUserInfo)
            }
          }

          private def createUser(
              createUser: UserCredentials
          )(implicit trx: Trx): ZIO[Any, Throwable, Either[ErrorResponse, EmptyResponse]] = {
            val passwordHash = determineHash(createUser.password)
            saveUser(createUser, passwordHash).flatMap {
              case Left(_) => ZIO.succeed(Left(ErrorResponse("Error while creating the user")))
              case _       => ZIO.succeed(Right(EmptyResponse()))
            }
          }

          override def handleUserAuthentication(
              credentials: UserCredentials
          )(
              implicit transactor: Trx
          ): ZIO[Any, Throwable, Either[UnauthorizedErrorResponse, AuthenticationResponse]] = {
            checkCredentials(credentials.name, determineHash(credentials.password))
              .flatMap {
                case Some(_) =>
                  ZIO.succeed(Right(AuthenticationResponse(tokenService.generateToken(credentials.name))))
                case None => ZIO.succeed(Left(UnauthorizedErrorResponse("Authentication failed")))
              }
          }

          override def checkUsernameExists(name: String)(implicit transactor: Trx): Task[Boolean] = {
            getFirstUserWithName(name).map(_.isDefined)
          }
        }
    )
}
