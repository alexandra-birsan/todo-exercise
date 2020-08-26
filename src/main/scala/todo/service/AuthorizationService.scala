package todo.service

import java.time.Clock

import todo.model.Models.JwtContent
import pdi.jwt._
import zio._
import io.circe.parser._
import io.circe.syntax._
import util.LoggingHelper.logErrorMessage
import todo.Trx
import todo.service.UserService.checkUsernameExists

object AuthorizationService {

  private val secret            = "secretKey"
  private val authorization     = JwtAlgorithm.HS256
  private val header            = JwtHeader(authorization)
  private val validityInSeconds = 180

  private implicit val clock: Clock = Clock.systemUTC

  def generateToken(username: String): String = {
    val content: String = JwtContent(username).asJson.noSpaces
    val claim = JwtClaim().withContent(content).expiresIn(validityInSeconds).issuedNow.startsNow
    Jwt.encode(header, claim, secret)
  }

  def isValidTokenForExistingUserAndExtractUser(
      token:             String
  )(implicit transactor: Trx): ZIO[Any, Throwable, Option[String]] = {
    isValidToken(token)
      .fold(
        _ => ZIO.succeed(Option.empty[String]),
        claim =>
          isClaimForExistingUser(claim)
            .flatMap { if (_) getUsernameFromClaim(claim) else ZIO.succeed(Option.empty[String]) }
      )
      .flatten[Any, Throwable, Option[String]]
  }

  private def isValidToken(token: String): ZIO[Any, Throwable, JwtClaim] = {
    ZIO
      .fromTry(Jwt.decode(token, secret, Seq(authorization)))
      .tapError(logErrorMessage)
  }

  private def isClaimForExistingUser(claim: JwtClaim)(implicit transactor: Trx) = {
    decode[JwtContent](claim.content)
      .fold(_ => ZIO.succeed(false), jwtContent => checkUsernameExists(jwtContent.owner))
      .tapError(logErrorMessage)
  }

  private def getUsernameFromClaim(claim: JwtClaim)(implicit transactor: Trx) = {
    decode[JwtContent](claim.content)
      .fold(
        _ => ZIO.succeed(Option.empty[String]),
        jwtContent =>
          checkUsernameExists(jwtContent.owner).map { if (_) Option(jwtContent.owner) else Option.empty[String] }
      )
      .tapError(logErrorMessage)
  }
}
