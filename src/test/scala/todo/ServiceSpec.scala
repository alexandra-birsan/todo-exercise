package todo

import java.time.Clock
import doobie.util.transactor.Transactor
import doobie.util.transactor.Transactor.Aux
import io.circe.syntax._
import org.http4s.{Header, HttpRoutes, Request, Response, Status}
import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim, JwtHeader}
import model.Models.JwtContent
import route.Routes
import todo.service.{AuthorizationService, TodoService, TokenService, UserService}
import todo.utils.TokenUtils
import zio.interop.catz._
import zio.test.Assertion.equalTo
import zio.test._
import zio.{ Has, Task, ZIO, ZLayer}

trait ServiceSpec {

  implicit val transactor: Aux[Task, Unit] = Transactor.fromDriverManager[Task](
    driver = "org.sqlite.JDBC",
    url    = "jdbc:sqlite:test.db"
  )

  val secret = "secretKey"
  val authorization: JwtAlgorithm.HS256.type = JwtAlgorithm.HS256
  val header:        JwtHeader               = JwtHeader(authorization)
  val userServiceLayer: ZLayer[Any, Throwable, Has[UserService.Service]] = TokenService.live >>> UserService.live
  val authorizationServiceLayer: ZLayer[Any, Throwable, Has[AuthorizationService.Service]] =
    userServiceLayer  >>> AuthorizationService.live
  val todoServiceLayer: ZLayer[Any, Throwable, Has[TodoService.Service]] =  authorizationServiceLayer >>> TodoService.live
  val routesServiceLayer: ZLayer[Any, Throwable,  Has[UserService.Service] with Has[TodoService.Service]] = todoServiceLayer ++ userServiceLayer
  val serverLayer: ZLayer[Any, Throwable, Has[Routes.Service]] =  routesServiceLayer >>> Routes.live
  val app: ZIO[Any, Throwable, HttpRoutes[Task]] = Routes.createRoutes().provideLayer(serverLayer ++ ZLayer.succeed(transactor))

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
    val value: ZIO[Any, Throwable, Option[Response[Task]]] =
      app.flatMap(_.run(finalRequest).value)
    assertM(value.map(_.get.status))(equalTo(Status.Unauthorized))
  }

  def withJwtWithOwnerNotAnExistingUser(implicit request: Request[Task]): ZIO[Any, Throwable, TestResult] = {
    val token        = TokenUtils.generateToken("Peter")
    val finalRequest = request.putHeaders(Header("Authorization", token))
    val value: ZIO[Any, Throwable, Option[Response[Task]]] =
      app.flatMap(_.run(finalRequest).value)
    assertM(value.map(_.get.status))(equalTo(Status.Unauthorized))  }

  def withInvalidAuthorizationHeader(implicit request: Request[Task]): ZIO[Any, Throwable, TestResult] = {
    val finalRequest = request.putHeaders(Header("Authorization", "dummy header"))
    val value: ZIO[Any, Throwable, Option[Response[Task]]] =
      app.flatMap(_.run(finalRequest).value)
    assertM(value.map(_.get.status))(equalTo(Status.Unauthorized))  }

  def withoutAuthorizationHeader(implicit request: Request[Task]): ZIO[Any, Throwable, BoolAlgebra[FailureDetails]] = {
    val value =  app.flatMap(_.run(request).value)
    for {
      isExpectedStatus <- assertM(value.map(_.get.status))(equalTo(Status.BadRequest))
      isExpectedBody <- assertM(getResponseBody(value))(equalTo("Invalid value for: header Authorization"))
    } yield isExpectedStatus && isExpectedBody
  }
}
