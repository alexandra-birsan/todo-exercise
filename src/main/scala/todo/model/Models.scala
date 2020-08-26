package todo.model

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

object Models {

  final case class Todo(
      id:   Int,
      name: String,
      done: Boolean
  )

  object Todo {
    implicit val circeCodec: Codec.AsObject[Todo] = deriveCodec[Todo]
  }

  final case class CreateTodo(name: String)

  object CreateTodo {
    implicit val circeCodec: Codec.AsObject[CreateTodo] = deriveCodec[CreateTodo]
  }

  final case class EmptyResponse()

  object EmptyResponse {
    implicit val circeCodec: Codec.AsObject[EmptyResponse] = deriveCodec[EmptyResponse]
  }

  sealed trait ErrorResp {}

  final case class ErrorResponse(message: String) extends ErrorResp

  object ErrorResponse {
    implicit val circeCodec: Codec.AsObject[ErrorResponse] = deriveCodec[ErrorResponse]
  }

  final case class UnauthorizedErrorResponse(message: String) extends ErrorResp

  object UnauthorizedErrorResponse {
    implicit val circeCodec: Codec.AsObject[UnauthorizedErrorResponse] = deriveCodec[UnauthorizedErrorResponse]

  }

  final case class ForbiddenResponse(message: String) extends ErrorResp

  object ForbiddenResponse {
    implicit val circeCodec: Codec.AsObject[ForbiddenResponse] = deriveCodec[ForbiddenResponse]

  }

  final case class User(id: Int, name: String, password: String)

  object User {
    implicit val circeCodex: Codec.AsObject[User] = deriveCodec[User]
  }

  case class Password(value: String)

  object Password {
    implicit val circeCodec: Codec.AsObject[Password] = deriveCodec[Password]
  }

  final case class UserCreation(name: String, password: String)

  object UserCreation {
    implicit val circeCodec: Codec.AsObject[UserCreation] = deriveCodec[UserCreation]
  }

  final case class AuthenticationResponse(token: String)

  object AuthenticationResponse {
    implicit val circeCodec: Codec.AsObject[AuthenticationResponse] = deriveCodec[AuthenticationResponse]

  }

  final case class JwtContent(owner: String)

  object JwtContent {
    implicit val circeCodec: Codec.AsObject[JwtContent] = deriveCodec[JwtContent]
  }
}
