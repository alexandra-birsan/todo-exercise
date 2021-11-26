package todo
package utils

import io.circe.syntax._
import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim, JwtHeader}
import model.Models.JwtContent

import java.time.Clock

object TokenUtils {

  private implicit val clock: Clock = Clock.systemUTC

  private val secret            = "secretKey"
  private val authorization     = JwtAlgorithm.HS256
  private val header            = JwtHeader(authorization)
  private val validityInSeconds = 180

  def generateToken(username:String) = {
    val content: String = JwtContent(username).asJson.noSpaces
    val claim = JwtClaim().withContent(content).expiresIn(validityInSeconds).issuedNow.startsNow
    Jwt.encode(header, claim, secret)
  }
}
