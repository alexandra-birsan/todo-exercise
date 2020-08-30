package todo

import org.http4s._
import todo.model.Models._
import todo.service.AuthorizationService
import zio._
import zio.test.Assertion._
import zio.test._
import io.circe.syntax._

object CreateTodoItemSpec extends ServiceSpec {

  implicit val request: Request[Task]#Self = Request[Task](Method.POST, Uri(path = "v1/todo"))
    .withEntity(CreateTodo("Buy tickets").asJson.noSpaces)

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = suite("Create Todo  item")(
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
    testM("with invalid todo item name") {
      val token = AuthorizationService.generateToken("John")
      val finalRequest = request
        .putHeaders(Header("Authorization", token))
        .withEntity(CreateTodo("").asJson.noSpaces)
      val value = app.run(finalRequest).value
      assertM(value.map(_.get.status))(equalTo(Status.BadRequest))
    },
    testM("when validations passed successfully") {
      val token               = AuthorizationService.generateToken("John")
      val finalRequest        = request.putHeaders(Header("Authorization", token))
      val value               = app.run(finalRequest).value
      val nextToDoIdAvailable = 4
      val idOfTheLoggedInUser = 1
      for {
        isExpectedStatus <- assertM(value.map(_.get.status))(equalTo(Status.Created))
        isToDoItemCreated <- assertM(DatabaseSetup.getToDoItem(nextToDoIdAvailable))(
          equalTo(Todo(nextToDoIdAvailable, "Buy tickets", done = false)))
        isExpectedOwner <- assertM(DatabaseSetup.getOwnerIdOfTheToDoItem(nextToDoIdAvailable))(
          equalTo(idOfTheLoggedInUser)
        )
      } yield isExpectedStatus && isToDoItemCreated && isExpectedOwner

    }
  )
}
