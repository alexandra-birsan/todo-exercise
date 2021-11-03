package todo
package route

import io.circe.syntax._
import org.http4s._
import model.Models.{CreateTodo, Todo}
import zio.{Runtime, Task, ZLayer}
import zio.test.Assertion.equalTo
import zio.test.{assertM, suite, testM, DefaultRunnableSpec, ZSpec}

object CreateTodoItemSpec extends DefaultRunnableSpec with ServiceSpec {

  implicit val request: Request[Task]#Self = Request[Task](Method.POST, Uri(path = "v1/todo"))
    .withEntity(CreateTodo("Buy tickets").asJson.noSpaces)

  private val runtime = Runtime.unsafeFromLayer(ZLayer.succeed(transactor))
  runtime.unsafeRun(DatabaseSetup.run.unit)
  runtime.shutdown()

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = suite("Create Todo  item")(
    testM("without the Authorization header") {
      withoutAuthorizationHeader
    },
    testM("with an invalid Authorization header") {
      withInvalidAuthorizationHeader
    },
    testM("with an Authorization header that does not belong to an existing user") {
      withJwtWithOwnerNotAnExistingUser
    },
    testM("with expired JWT") {
      withExpiredJwt
    },
    testM("with invalid todo item name") {
      val token = authorizationService.generateToken("John")
      val finalRequest = request
        .putHeaders(Header("Authorization", token))
        .withEntity(CreateTodo("").asJson.noSpaces)
      val value = app.run(finalRequest).value
      assertM(value.map(_.get.status))(equalTo(Status.BadRequest))
    },
    testM("when all the validations are successfully passed") {
      val token               = authorizationService.generateToken("John")
      val finalRequest        = request.putHeaders(Header("Authorization", token))
      val value               = app.run(finalRequest).value
      val nextToDoIdAvailable = 5
      val idOfTheLoggedInUser = 1
      for {
        isExpectedStatus <- assertM(value.map(_.get.status))(equalTo(Status.Created))
        isToDoItemCreated <- assertM(DatabaseSetup.getToDoItem(nextToDoIdAvailable))(
          equalTo(Todo(nextToDoIdAvailable, "Buy tickets", done = false))
        )
        isExpectedOwner <- assertM(DatabaseSetup.getOwnerIdOfTheToDoItem(nextToDoIdAvailable))(
          equalTo(idOfTheLoggedInUser)
        )
      } yield isExpectedStatus && isToDoItemCreated && isExpectedOwner

    }
  )
}
