package todo

import doobie.implicits._

import zio._
import zio.interop.catz._

object InitialDatabaseSetup {

  private val createTodoTable = {
    sql"""
         |create table if not exists todo(
         |    id    integer primary key,
         |    name  text    not null,
         |    owner_id integer    not null,
         |    done  tinyint not null default 0,
         |    foreign key (owner_id) references users(id)
         |)""".stripMargin
  }

  private val createUsersTable = {
    sql"""
         |create table if not exists  users(
         |id integer primary key,
         |name text not null,
         |password text not null
         |)
         |""".stripMargin
  }

  def getOwnerOfTheTodoItem(id:Int)(implicit transactor: Trx): Task[Option[String]] = {
    sql"""select users.name from users where users.id  = (SELECT owner_id from todo  where todo.id = ${id})"""
      .stripMargin
      .query[String]
      .option
      .transact(transactor)
  }

  def run: URIO[Transactional, Unit] = {
    ZIO.service[Trx].flatMap { transactor =>
      createTodoTable.update.run.transact(transactor).unit.orDie
      createUsersTable.update.run.transact(transactor).unit.orDie
    }
  }

}
