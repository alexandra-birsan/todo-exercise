package todo

import java.time.Clock

import doobie.util.transactor.Transactor
import doobie.util.transactor.Transactor.Aux
import io.circe.syntax._
import org.http4s.{Header, HttpRoutes, Request, Response, Status}
import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim, JwtHeader}
import todo.model.Models.JwtContent
import todo.route.Routes
import todo.service.AuthorizationService
import zio.interop.catz._
import zio.test.Assertion.equalTo
import zio.test._
import zio.{Task, ZIO}

trait ServiceSpec extends DefaultRunnableSpec {

  implicit val transactor: Aux[Task, Unit] = Transactor.fromDriverManager[Task](
    driver = "org.sqlite.JDBC",
    url    = "jdbc:sqlite:test.db"
  )

  val secret        = "secretKey"
  val authorization: JwtAlgorithm.HS256.type = JwtAlgorithm.HS256
  val header: JwtHeader = JwtHeader(authorization)
  val app: HttpRoutes[Task] = Routes.routes

  override def runner: TestRunner[_root_.zio.test.environment.TestEnvironment, Any] = super.runner

  def getResponseBody(value: Task[Option[Response[Task]]]): ZIO[Any, Throwable, String] = {
    for {
      response <- value
      body <- response.get.body.compile.toVector.map(x => x.map(_.toChar).mkString(""))
    } yield body
  }

  def createExpiredJwt: String = {
    implicit val clock: Clock = Clock.systemUTC

    val content: String = JwtContent("John").asJson.noSpaces
    val claim = JwtClaim().withContent(content).expiresNow
    Jwt.encode(header, claim, secret)
  }

  def withExpiredJwt(implicit request: Request[Task]): ZIO[Any, Throwable, TestResult] = {
    val token        = createExpiredJwt
    val finalRequest = request.putHeaders(Header("Authorization", token))
    val value        = app.run(finalRequest).value
    assertM(value.map(_.get.status))(equalTo(Status.Unauthorized))
  }

  def withJwtWithOwnerNotAnExistingUser(implicit request: Request[Task]): ZIO[Any, Throwable, TestResult] = {
    val token        = AuthorizationService.generateToken("Peter")
    val finalRequest = request.putHeaders(Header("Authorization", token))
    assertM(app.run(finalRequest).value.map(_.get.status))(equalTo(Status.Unauthorized))
  }

  def withInvalidAuthorizationHeader(implicit request: Request[Task]): ZIO[Any, Throwable, TestResult] = {
    val finalRequest = request.putHeaders(Header("Authorization", "dummy header"))
    assertM(app.run(finalRequest).value.map(_.get.status))(equalTo(Status.Unauthorized))
  }

  def withoutAuthorizationHeader(implicit request: Request[Task]): ZIO[Any, Throwable, BoolAlgebra[FailureDetails]] = {
    val value = app.run(request).value
    for {
      isExpectedStatus <- assertM(value.map(_.get.status))(equalTo(Status.BadRequest))
      isExpectedBody <- assertM(getResponseBody(value))(equalTo("Invalid value for: header Authorization"))
    } yield isExpectedStatus && isExpectedBody
  }
}
