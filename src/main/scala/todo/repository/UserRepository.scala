package todo.repository

import java.sql.SQLException

import doobie.implicits._
import todo.util.LoggingHelper.logErrorMessage
import todo.model.Models.UserCredentials
import todo.Trx
import zio.{Task, ZIO}
import zio.interop.catz._

object UserRepository {

  def saveUser(createUser: UserCredentials, password: String)(
      implicit transactor: Trx
  ): ZIO[Any, Throwable, Either[SQLException, Int]] = {
    sql"""insert into users (name, password)
         | values (${createUser.name}, $password)""".stripMargin.update.run.attemptSql
      .transact(transactor)
      .tapError(logErrorMessage)
  }

  def checkCredentials(name: String, password: String)(
      implicit transactor:   Trx
  ): ZIO[Any, Throwable, Option[String]] = {
    sql"""select 1 from users
         | where name = $name AND password = $password""".stripMargin
      .query[String]
      .option
      .transact(transactor)
      .tapError(logErrorMessage)
  }

  def getFirstUserWithName(name: String)(implicit transactor: Trx): Task[Option[String]] = {
    sql"""SELECT users.name from users
         |  where name = $name""".stripMargin
      .query[String]
      .option
      .transact(transactor)
      .tapError(logErrorMessage)
  }

}
