package todo.service

import java.time.Clock
import todo.model.Models.JwtContent
import pdi.jwt._
import zio._
import io.circe.parser._
import io.circe.syntax._
import todo.util.LoggingHelper.logErrorMessage
import todo.Trx

object AuthorizationService {

  trait Service {

    def generateToken(username:                          String): String
    def isValidTokenForExistingUserAndExtractUser(token: String)(
        implicit transactor:                             Trx
    ): ZIO[Any, Throwable, Option[String]]
  }
}

trait AuthorizationService {

  val authorizationService: AuthorizationService.Service
}

trait AuthorizationServiceLive extends AuthorizationService with UserService {

  override val authorizationService: AuthorizationService.Service = new AuthorizationService.Service {

    private val secret            = "secretKey"
    private val authorization     = JwtAlgorithm.HS256
    private val header            = JwtHeader(authorization)
    private val validityInSeconds = 180

    private implicit val clock: Clock = Clock.systemUTC

    override def generateToken(username: String): String = {
      val content: String = JwtContent(username).asJson.noSpaces
      val claim = JwtClaim().withContent(content).expiresIn(validityInSeconds).issuedNow.startsNow
      Jwt.encode(header, claim, secret)
    }

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
}
