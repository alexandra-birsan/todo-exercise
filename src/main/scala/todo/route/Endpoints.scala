package todo.route

import sttp.model.StatusCode
import sttp.tapir.json.circe.jsonBody
import sttp.tapir._
import todo.model.Models._

object Endpoints {

  type AuthToken = String
  private val baseV1Endpoint               = endpoint.in("v1")
  private val authorizationHeader          = header[AuthToken]("Authorization")
  private val baseV1TodoAuthorizedEndpoint = baseV1Endpoint.in(authorizationHeader).in("todo")
  private val baseV1UserEndpoint           = baseV1Endpoint.in("auth")

  val listTodos: Endpoint[AuthToken, ErrorResp, List[Todo], Nothing] = {
    baseV1TodoAuthorizedEndpoint.get
      .out(jsonBody[List[Todo]])
      .errorOut(
        oneOf[ErrorResp](
          unAuthorizeErrorResponseMapping,
          forbiddenResponseMapping,
          statusDefaultMapping(jsonBody[ErrorResponse])
        )
      )
  }

  val createTodo: Endpoint[(AuthToken, CreateTodo), UnauthorizedErrorResponse, EmptyResponse, Nothing] = {
    baseV1TodoAuthorizedEndpoint.post
      .in(
        jsonBody[CreateTodo]
          .validate(Validator.minLength(1).contramap(_.name))
      )
      .out(oneOf(creationResponseStatusMapping))
      .errorOut(
        oneOf(
          unAuthorizeErrorResponseMapping
        )
      )
  }

  val finishTodo: Endpoint[(AuthToken, Int), ErrorResp, EmptyResponse, Nothing] = {
    baseV1TodoAuthorizedEndpoint.put
      .in(path[Int]("id"))
      .out(jsonBody[EmptyResponse])
      .errorOut(
        oneOf[ErrorResp](
          unAuthorizeErrorResponseMapping,
          forbiddenResponseMapping
        )
      )
  }

  private def forbiddenResponseMapping = {
    statusMapping(StatusCode.Forbidden, jsonBody[ForbiddenResponse])
  }

  val createUser: Endpoint[UserCredentials, ErrorResponse, EmptyResponse, Nothing] = {
    baseV1UserEndpoint.post
      .in("new")
      .in(
        jsonBody[UserCredentials]
          .validate(Validator.minLength(3).contramap(_.name))
          .validate(Validator.minLength(5).contramap(_.password))
      )
      .out(oneOf(creationResponseStatusMapping))
      .errorOut(jsonBody[ErrorResponse])
  }

  private def creationResponseStatusMapping = {
    statusMapping(StatusCode.Created, jsonBody[EmptyResponse])
  }

  val authenticateUser: Endpoint[UserCredentials, UnauthorizedErrorResponse, AuthenticationResponse, Nothing] = {
    baseV1UserEndpoint.post
      .in(jsonBody[UserCredentials])
      .out(jsonBody[AuthenticationResponse])
      .errorOut(oneOf(unAuthorizeErrorResponseMapping))
  }

  private def unAuthorizeErrorResponseMapping = {
    statusMapping(StatusCode.Unauthorized, jsonBody[UnauthorizedErrorResponse])
  }
}
