package todo.service

import todo.model.Models.JwtContent
import pdi.jwt._
import zio._
import io.circe.parser._
import todo.util.LoggingHelper.logErrorMessage
import todo.Trx

object AuthorizationService {

  trait Service {

    def isValidTokenForExistingUserAndExtractUser(token: String)(
        implicit transactor:                             Trx
    ): ZIO[Any, Throwable, Option[String]]
  }

  val live: ZLayer[Has[UserService.Service], Throwable, Has[AuthorizationService.Service]] =
    ZLayer.fromService(
      userService =>
        new AuthorizationService.Service {

          private val secret        = "secretKey"
          private val authorization = JwtAlgorithm.HS256

          override def isValidTokenForExistingUserAndExtractUser(
              token:             String
          )(implicit transactor: Trx): ZIO[Any, Throwable, Option[String]] = {
            isValidToken(token)
              .foldM(
                _ => ZIO.succeed(Option.empty[String]),
                claim =>
                  isClaimForExistingUser(claim)
                    .flatMap { if (_) getUsernameFromClaim(claim) else ZIO.succeed(Option.empty[String]) }
              )
          }

          private def isValidToken(token: String): ZIO[Any, Throwable, JwtClaim] = {
            ZIO
              .fromTry(Jwt.decode(token, secret, Seq(authorization)))
              .tapError(logErrorMessage)
          }

          private def isClaimForExistingUser(claim: JwtClaim)(implicit transactor: Trx) = {
            decode[JwtContent](claim.content)
              .fold(_ => ZIO.succeed(false), jwtContent => userService.checkUsernameExists(jwtContent.owner))
              .tapError(logErrorMessage)
          }

          private def getUsernameFromClaim(claim: JwtClaim)(implicit transactor: Trx) = {
            decode[JwtContent](claim.content)
              .fold(
                _ => ZIO.succeed(Option.empty[String]),
                jwtContent =>
                  userService.checkUsernameExists(jwtContent.owner).map {
                    if (_) Option(jwtContent.owner) else Option.empty[String]
                  }
              )
              .tapError(logErrorMessage)
          }
        }
    )
}
