package todo.service

import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim, JwtHeader}
import todo.model.Models.JwtContent
import zio.{Has, ULayer, ZLayer}

import java.time.Clock
import io.circe.syntax._

object TokenService {

  private implicit val clock: Clock = Clock.systemUTC

  private val secret            = "secretKey"
  private val authorization     = JwtAlgorithm.HS256
  private val header            = JwtHeader(authorization)
  private val validityInSeconds = 180

  trait Service {

    def generateToken(username: String): String
  }

  val live: ULayer[Has[Service]] = ZLayer.succeed((username: String) => {
    val content: String = JwtContent(username).asJson.noSpaces
    val claim = JwtClaim().withContent(content).expiresIn(validityInSeconds).issuedNow.startsNow
    Jwt.encode(header, claim, secret)
  })

}
