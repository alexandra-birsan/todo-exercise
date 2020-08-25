package todo.repository

import doobie.implicits._

import todo.LoggingHelper.logErrorMessage
import todo.Models.{CreateTodo, Todo}
import todo.Trx

import zio.ZIO
import zio.interop.catz._

object TodoRepository {

  def saveTodoItem(createTodo: CreateTodo, loggedInUser: Option[String])(implicit transactor: Trx): ZIO[Any, Throwable, Unit] = {
    sql"""INSERT INTO todo (name, owner_id) VALUES
         |(${createTodo.name}, (SELECT id from users WHERE name=${loggedInUser.get})
         |)""".stripMargin
      .update
      .run
      .transact(transactor)
      .unit
      .tapError(logErrorMessage)
  }

  def finishTodoItem(id: Int)(implicit transactor: Trx): ZIO[Any, Throwable, Int] = {
    sql"""update todo set done = 1 where id = ${id}""".stripMargin
      .update
      .run
      .transact(transactor)
      .tapError(logErrorMessage)
  }

   def listTodoItems(loggedInUser: Option[String])(implicit transactor:Trx): ZIO[Any, Throwable, List[Todo]] = {
    sql"""select id, name, done from todo
          | where owner_id = (
          | select id from users where users.name = ${loggedInUser.get}
          | )""".stripMargin
      .query[Todo]
      .to[List]
      .transact(transactor)
      .tapError(logErrorMessage)
  }

}
