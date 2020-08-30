package todo

import org.http4s.{Header, Method, Request, Status, Uri}
import todo.model.Models.Todo
import todo.service.AuthorizationService
import zio.Task
import zio.test.Assertion.{equalTo, isTrue}
import zio.test.{Assertion, TestFailure, ZSpec, assertM, suite, testM, _}

object FinishTodoItemSpec extends ServiceSpec {

  private implicit val request: Request[Task] = Request[Task](Method.PUT, Uri(path = "v1/todo/1000"))

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = suite("Mark todo item as finished")(
    testM("without the Authorization header") {
      withoutAuthorizationHeader
    },
    testM("with invalid Authorization header") {
      withInvalidAuthorizationHeader
    },
    testM("with an Authorization header that does not belong to an existing user") {
      withJwtWithOwnerNotAnExistingUser
    },
    testM("when expired JWT") {
      withExpiredJwt
    },
    testM("with the id path parameter not corresponding to any stored todo item") {
      val token   = AuthorizationService.generateToken("John")
      val finalRequest = request.putHeaders(Header("Authorization", token))
      val value   = app.run(finalRequest).value
      assertM(value.map(_.get.status))(equalTo(Status.Forbidden))
    },
    testM("with the JWT not belonging to the owner of the todo item") {
      val token   = AuthorizationService.generateToken("John")
      val finalRequest = Request[Task](Method.PUT, Uri(path = "v1/todo/3")).putHeaders(Header("Authorization", token))
      val value   = app.run(finalRequest).value
      assertM(value.map(_.get.status))(equalTo(Status.Forbidden))
    },
    testM("with the JWT not belonging to the owner of the todo item") {
      val token   = AuthorizationService.generateToken("John")
      val request = Request[Task](Method.PUT, Uri(path = "v1/todo/3")).putHeaders(Header("Authorization", token))
      val value   = app.run(request).value
      assertM(value.map(_.get.status))(equalTo(Status.Forbidden))
    },
    testM("with the JWT belonging to the owner of the todo item") {
      val token  = AuthorizationService.generateToken("John")
      val todoId = "1"
      val request =
        Request[Task](Method.PUT, Uri(path = "v1/todo/" + todoId)).putHeaders(Header("Authorization", token))
      val value = app.run(request).value
      for {
        isExpectedStatus <- assertM(value.map(_.get.status))(equalTo(Status.Ok))
        isExpectedBody <- assertM(getResponseBody(value))(equalTo("{}"))
        isUpdatedResult <- assertM(DatabaseSetup.getToDoItem(todoId.toInt))(equalTo(Todo(1, "Smile more often!", true)))
      } yield isExpectedStatus && isExpectedBody && isUpdatedResult
    }
  )

}
