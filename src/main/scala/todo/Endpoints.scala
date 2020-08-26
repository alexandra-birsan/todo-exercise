package todo

import sttp.tapir.json.circe.jsonBody
import sttp.tapir._

import sttp.model.StatusCode

import todo.Models._

object Endpoints {

  type AuthToken = String
  private val authorizationHeader = header[AuthToken]("Authorization")

//  val baseEndpoint: Endpoint[Unit, ErrorResp, Unit, Nothing] =
//    endpoint
//      .errorOut(
//        oneOf[Either[ErrorResp]](
//          statusMappingFromMatchType(StatusCode.Unauthorized, jsonBody[UnAuthorizedErrorResponse]),
//          statusMappingFromMatchType(StatusCode.Forbidden, jsonBody[ForbiddenResponse]),
//        )
//      )

  val listTodos: Endpoint[AuthToken, ErrorResp, List[Todo], Nothing] = {
    endpoint.get
      .in("v1" / "todo")
      .in(authorizationHeader)
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
    endpoint.post
      .in("v1" / "todo")
      .in(authorizationHeader)
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

  val finishTodo: Endpoint[(Int, AuthToken), ErrorResp, EmptyResponse, Nothing] = {
    endpoint.put
      .in("v1" / "todo" / path[Int]("id"))
      .in(authorizationHeader)
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

  val createUser: Endpoint[UserCreation, ErrorResponse, EmptyResponse, Nothing] = {
    endpoint.post
      .in("v1" / "auth" / "new")
      .in(
        jsonBody[UserCreation]
          .validate(Validator.minLength(3).contramap(_.name))
          .validate(Validator.minLength(5).contramap(_.password))
      )
      .out(oneOf(creationResponseStatusMapping))
      .errorOut(jsonBody[ErrorResponse])
  }

  private def creationResponseStatusMapping = {
    statusMapping(StatusCode.Created, jsonBody[EmptyResponse])
  }

  val authenticateUser: Endpoint[UserCreation, UnauthorizedErrorResponse, AuthenticationResponse, Nothing] = {
    endpoint.post
      .in("v1" / "auth")
      .in(jsonBody[UserCreation])
      .out(jsonBody[AuthenticationResponse])
      .errorOut(oneOf(unAuthorizeErrorResponseMapping))
  }

  private def unAuthorizeErrorResponseMapping = {
    statusMapping(StatusCode.Unauthorized, jsonBody[UnauthorizedErrorResponse])
  }
}
