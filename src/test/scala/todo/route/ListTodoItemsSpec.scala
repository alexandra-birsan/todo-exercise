package todo
package route

import io.circe.syntax._
import org.http4s._
import model.Models.Todo
import utils.TokenUtils
import zio.{Runtime, Task, ZLayer}
import zio.test.Assertion.equalTo
import zio.test.{DefaultRunnableSpec, ZSpec, assertM, suite, testM}

object ListTodoItemsSpec extends DefaultRunnableSpec with ServiceSpec {

  private implicit val request: Request[Task] = Request[Task](Method.GET, Uri(path = "v1/todo"))
  private val runtime = Runtime.unsafeFromLayer(ZLayer.succeed(transactor))
  runtime.unsafeRun(DatabaseSetup.run.unit)
  runtime.shutdown()

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = suite("List Todo  items")(
    testM("without the Authorization header") {
      withoutAuthorizationHeader
    },
    testM("with an invalid Authorization header") {
      withInvalidAuthorizationHeader
    },
    testM("with an Authorization header that does not belong to an existing user") {
      withJwtWithOwnerNotAnExistingUser
    },
    testM("with an expired JWT") {
      withExpiredJwt
    },
    testM("with an Authorization header that belongs to an existing user") {
      val token        = TokenUtils.generateToken("Alex")
      val finalRequest = request.putHeaders(Header("Authorization", token))
      val value = app.flatMap(_.run(finalRequest).value)
      for {
        isExpectedStatus <- assertM(value.map(_.get.status))(equalTo(Status.Ok))
        isExpectedBody <- assertM(getResponseBody(value))(
          equalTo(
            List(Todo(3, "Read at least 15 minutes a day", done = true), Todo(4, "Buy a cake", done = false)).asJson.noSpaces
          )
        )
      } yield isExpectedStatus && isExpectedBody

    }
  )
}
