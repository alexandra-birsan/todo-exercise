package todo

import zio.{Task, ZIO}

object LoggingHelper {

  def logErrorMessage: Throwable => Task[Unit] = (e:Throwable) => ZIO.effect(println(e.getMessage))
}
