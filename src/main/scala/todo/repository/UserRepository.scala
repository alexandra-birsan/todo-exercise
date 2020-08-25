package todo.repository

import java.sql.SQLException

import doobie.implicits._

import todo.LoggingHelper.logErrorMessage
import todo.Models.UserCreation
import todo.Trx

import zio.{Task, ZIO}
import zio.interop.catz._

object UserRepository {

  def saveUser(createUser: UserCreation, password: String)
              (implicit transactor: Trx): ZIO[Any, Throwable, Either[SQLException, Int]] = {
    sql"""insert into users (name, password)
         | values (${createUser.name}, ${password})""".stripMargin
      .update
      .run
      .attemptSql
      .transact(transactor)
      .tapError(logErrorMessage)
  }

  def checkCredentials(name: String, password: String): doobie.ConnectionIO[Int] = {
    sql"""select count(*) from users
         | where name = ${name} AND password = ${password}""".stripMargin
      .query[Int]
      .unique
  }

  def getFirstUserWithName(name: String)(implicit transactor: Trx): Task[Option[String]] = {
    sql"""SELECT users.name from users
         |  where name = ${name} LIMIT 1""".stripMargin
      .query[String]
      .option
      .transact(transactor)
      .tapError(logErrorMessage)
  }

}
