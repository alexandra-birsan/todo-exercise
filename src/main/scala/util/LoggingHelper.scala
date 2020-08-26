package util

import zio.{Task, ZIO}

object LoggingHelper {

  def logErrorMessage: Throwable => Task[Unit] = (e: Throwable) => ZIO.effect(println(e.getMessage))

  def logMessage: String => Task[Unit] = (e: String) => ZIO.effect(println(e))

}
