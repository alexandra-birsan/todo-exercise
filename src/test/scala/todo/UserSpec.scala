package todo

import org.http4s._
import todo.model.Models._
import todo.service.AuthorizationService
import zio._
import zio.test.Assertion._
import zio.test._
import io.circe.syntax._
import pdi.jwt.{Jwt, JwtClaim}
import todo.ListTodoItemsSpec.getResponseBody
import todo.UserSpec.getResponseBody
import todo.service.AuthorizationService.{authorization, secret}
import todo.util.LoggingHelper.logErrorMessage
import zio.interop.catz._
import io.circe.parser.decode

import scala.util.Try

object UserSpec extends ServiceSpec {

  private implicit val request: Request[Task] = Request[Task](Method.POST, Uri(path = "v1/auth"))

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = suite("User  service")(
    testM("create user when username already exists") {
      val finalRequest =
        request.withUri(Uri(path = "v1/auth/new")).withEntity(UserCredentials("John", "secret123").asJson.noSpaces)
      val value = app.run(finalRequest).value
      for {
        isExpectedStatus <- assertM(value.map(_.get.status))(equalTo(Status.BadRequest))
        isExpectedBody <- assertM(getResponseBody(value))(equalTo(ErrorResponse("User already exists").asJson.noSpaces))
      } yield isExpectedStatus && isExpectedBody
    },
    testM("create user with invalid username") {
      val finalRequest =
        request.withUri(Uri(path = "v1/auth/new")).withEntity(UserCredentials("a", "secret123").asJson.noSpaces)
      val value = app.run(finalRequest).value
      assertM(value.map(_.get.status))(equalTo(Status.BadRequest))
    },
    testM("create user with invalid password") {
      val finalRequest =
        request.withUri(Uri(path = "v1/auth/new")).withEntity(UserCredentials("Isac", "9").asJson.noSpaces)
      val value = app.run(finalRequest).value
      assertM(value.map(_.get.status))(equalTo(Status.BadRequest))
    },
    testM("create user when all validations successfully passed") {
      val finalRequest =
        request.withUri(Uri(path = "v1/auth/new")).withEntity(UserCredentials("Isac", "secret123*").asJson.noSpaces)
      val value = app.run(finalRequest).value
      assertM(value.map(_.get.status))(equalTo(Status.Created))
    },
    testM("log in with invalid credentials") {
      val finalRequest =
        request.withEntity(UserCredentials("John", "secret123*").asJson.noSpaces)
      val value = app.run(finalRequest).value
      assertM(value.map(_.get.status))(equalTo(Status.Unauthorized))
    },
    testM("log in successfully") {
      val finalRequest =
        request.withEntity(UserCredentials("John", "secret").asJson.noSpaces)
      val value = app.run(finalRequest).value
      for {
        isExpectedStatus <- assertM(value.map(_.get.status))(equalTo(Status.Ok))
        jwtContent = getResponseBody(value)
          .map {
            decode[AuthenticationResponse](_).getOrElse[AuthenticationResponse](AuthenticationResponse("invalid"))
          }
          .map(authResponse => Jwt.decode(authResponse.token, secret, Seq(authorization)).map(_.content))
        isExpectedJwtContent <- assertM(jwtContent)(Assertion.equalTo(Try(JwtContent("John").asJson.noSpaces)))
      } yield isExpectedStatus && isExpectedJwtContent
    }
  )
}
