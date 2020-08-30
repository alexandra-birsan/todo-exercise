package todo

import io.circe.syntax._
import org.http4s.{Request, _}
import todo.model.Models.Todo
import todo.service.AuthorizationService
import zio.Task
import zio.test.Assertion._
import zio.test._

object ListTodoItemsSpec extends ServiceSpec {

  private implicit val request: Request[Task] = Request[Task](Method.GET, Uri(path = "v1/todo"))

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = suite("List Todo  items")(
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
    testM("with an Authorization header that belongs to an existing user") {
      val token   = AuthorizationService.generateToken("John")
      val finalRequest = request.putHeaders(Header("Authorization", token))
      val value   = app.run(finalRequest).value
      for{
      isExpectedStatus <-  assertM(value.map(_.get.status))(equalTo(Status.Ok))
      isExpectedBody <- assertM(getResponseBody(value))(
        equalTo(List(Todo(1, "Smile more often!", done = false), Todo(2, "Be nice!", done = true)).asJson.noSpaces))
      } yield isExpectedStatus && isExpectedBody

    }
  )
}
