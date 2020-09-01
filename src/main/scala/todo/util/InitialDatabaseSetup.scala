package todo.util

import doobie.implicits._
import doobie.util.fragment
import todo.{Transactional, Trx}
import zio._
import zio.interop.catz._

object InitialDatabaseSetup {

   val createTodoTable: fragment.Fragment = {
    sql"""
         |create table if not exists todo(
         |    id    integer primary key,
         |    name  text    not null,
         |    owner_id integer    not null,
         |    done  tinyint not null default 0,
         |    foreign key (owner_id) references users(id)
         |)""".stripMargin
  }

   val createUsersTable: fragment.Fragment = {
    sql"""
         |create table if not exists  users(
         |id integer primary key,
         |name text not null,
         |password text not null
         |)
         |""".stripMargin
  }

  def run: URIO[Transactional, Unit] = {
    ZIO.service[Trx].flatMap { transactor =>
      createTodoTable.update.run.transact(transactor).unit.orDie
      createUsersTable.update.run.transact(transactor).unit.orDie
    }
  }
}
