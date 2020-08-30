package todo

import doobie.implicits._
import todo.model.Models.Todo
import todo.util.LoggingHelper.logErrorMessage
import util.InitialDatabaseSetup.{createTodoTable, createUsersTable}
import zio.ZIO
import zio.interop.catz._

object DatabaseSetup {

  private val cleanUpUsersTable = {
    sql"""delete from users""".stripMargin
  }

  private val cleanUpTodoTable = {
    sql"""delete from todo""".stripMargin
  }

  private val createUsers = {
    sql"""insert into users (name, password)
         | values ('John', '5ebe2294ecd0e0f08eab7690d2a6ee69'),
         | ('Alex', '47202bb8fc8cc53a7885001a485e2704')""".stripMargin
  }

  private val createToDoItems = {
    sql"""INSERT INTO todo (name, owner_id, done) VALUES
         |('Smile more often!', 1, false),
         |('Be nice!', 1, true),
         |('Read at least 15 minutes a day',2, false)
         """.stripMargin
  }

  def run(implicit transactor: Trx): ZIO[Transactional, Throwable, Unit] = {
    createTodoTable.update.run.transact(transactor).unit *>
      createUsersTable.update.run.transact(transactor).unit *>
      cleanUpTodoTable.update.run.transact(transactor).unit *>
      cleanUpUsersTable.update.run.transact(transactor).unit *>
      createUsers.update.run.transact(transactor).unit *>
      createToDoItems.update.run.transact(transactor).unit
  }

  def getToDoItem(id: Int)(implicit transactor: Trx): ZIO[Any, Throwable, Todo] = {
    sql"""select id, name, done from todo
         | where id = $id""".stripMargin
      .query[Todo]
      .unique
      .transact(transactor)
      .tapError(logErrorMessage)
  }

  def getOwnerIdOfTheToDoItem(id: Int)(implicit transactor: Trx): ZIO[Any, Throwable, Int] = {
    sql"""select owner_id from todo
         | where id = $id""".stripMargin
      .query[Int]
      .unique
      .transact(transactor)
      .tapError(logErrorMessage)
  }
}
