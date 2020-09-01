package todo.route

import io.circe.parser._
import io.circe.syntax._
import org.http4s.{Method, Request, Status, Uri}
import pdi.jwt.Jwt
import todo.{DatabaseSetup, ServiceSpec}
import todo.model.Models._
import zio.{Runtime, Task, ZLayer}
import zio.test.Assertion.equalTo
import zio.test._

import scala.util.Try

object UserSpec extends DefaultRunnableSpec with ServiceSpec {

  private implicit val request: Request[Task] = Request[Task](Method.POST, Uri(path = "v1/auth"))
  private val runtime = Runtime.unsafeFromLayer(ZLayer.succeed(transactor))
  runtime.unsafeRun(DatabaseSetup.run.unit)
  runtime.shutdown()

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = suite("User  service")(
    testM("create user when the username already exists") {
      val finalRequest =
        request.withUri(Uri(path = "v1/auth/new")).withEntity(UserCredentials("John", "secret123").asJson.noSpaces)
      val value = app.run(finalRequest).value
      for {
        isExpectedStatus <- assertM(value.map(_.get.status))(equalTo(Status.BadRequest))
        isExpectedBody <- assertM(getResponseBody(value))(equalTo(ErrorResponse("User already exists").asJson.noSpaces))
      } yield isExpectedStatus && isExpectedBody
    },
    testM("create user with an invalid username") {
      val finalRequest =
        request.withUri(Uri(path = "v1/auth/new")).withEntity(UserCredentials("a", "secret123").asJson.noSpaces)
      val value = app.run(finalRequest).value
      assertM(value.map(_.get.status))(equalTo(Status.BadRequest))
    },
    testM("create user with an invalid password") {
      val finalRequest =
        request.withUri(Uri(path = "v1/auth/new")).withEntity(UserCredentials("Isac", "9").asJson.noSpaces)
      val value = app.run(finalRequest).value
      assertM(value.map(_.get.status))(equalTo(Status.BadRequest))
    },
    testM("create user when all the validations are successfully  passed") {
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
