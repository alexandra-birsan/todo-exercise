package todo.route

import org.http4s._
import todo.model.Models.Todo
import todo.service.AuthorizationService
import todo.{DatabaseSetup, ServiceSpec}
import zio.{Runtime, Task, ZLayer}
import zio.test.Assertion.equalTo
import zio.test.{assertM, suite, testM, DefaultRunnableSpec, ZSpec}

object FinishTodoItemSpec extends DefaultRunnableSpec with ServiceSpec {

  private implicit val request: Request[Task] = Request[Task](Method.PUT, Uri(path = "v1/todo/1000"))
  private val runtime = Runtime.unsafeFromLayer(ZLayer.succeed(transactor))
  runtime.unsafeRun(DatabaseSetup.run.unit)
  runtime.shutdown()

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = suite("Mark todo item as finished")(
    testM("without the Authorization header") {
      withoutAuthorizationHeader
    },
    testM("with invalid Authorization header") {
      withInvalidAuthorizationHeader
    },
    testM("with an Authorization header that does not belong to any existing user") {
      withJwtWithOwnerNotAnExistingUser
    },
    testM("with an expired JWT") {
      withExpiredJwt
    },
    testM("when the todo id parameter does not correspond to any existing todo item") {
      val token        = AuthorizationService.generateToken("John")
      val finalRequest = request.putHeaders(Header("Authorization", token))
      val value        = app.run(finalRequest).value
      assertM(value.map(_.get.status))(equalTo(Status.Forbidden))
    },
    testM("when the todo item does not belong to the owner of the JWT") {
      val token        = AuthorizationService.generateToken("John")
      val finalRequest = Request[Task](Method.PUT, Uri(path = "v1/todo/3")).putHeaders(Header("Authorization", token))
      val value        = app.run(finalRequest).value
      assertM(value.map(_.get.status))(equalTo(Status.Forbidden))
    },
    testM("when the todo item belongs to the owner of the JWT") {
      val token   = AuthorizationService.generateToken("John")
      val todoId  = "1"
      val request = Request[Task](Method.PUT, Uri(path = "v1/todo/1")).putHeaders(Header("Authorization", token))
      val value   = app.run(request).value
      for {
        isExpectedStatus <- assertM(value.map(_.get.status))(equalTo(Status.Ok))
        isExpectedBody <- assertM(getResponseBody(value))(equalTo("{}"))
        isUpdatedResult <- assertM(DatabaseSetup.getToDoItem(todoId.toInt))(
          equalTo(Todo(todoId.toInt, "Smile more often!", done = true))
        )
      } yield isExpectedStatus && isExpectedBody && isUpdatedResult
    }
  )

}
