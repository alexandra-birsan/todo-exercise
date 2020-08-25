package todo

import zio.{App, ExitCode, URIO, ZIO}

object PlayingWithZio {

}

object HelloWorld extends App {
  import zio.console._

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    putStr("Hello world!") as ExitCode.success
}

object PrintSequence extends App {
  import zio.console._

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    putStr("Hello ") *> (putStr(" world!")) *> ZIO.succeed(ExitCode.success)
}

object ErrorRecovery extends App {
  val SdtInputFailed = 1

  import zio.console._

  val failed = putStrLn("About to fail") *>
    ZIO.fail("Uh oh") *>
    putStrLn("This will never be printed")

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
  //    failed as ExitCode.success orElse ZIO.succeed(1)
  //  failed.fold(_ => ExitCode.failure, _ => ExitCode.success )
    (failed as ExitCode.success).catchAllCause(cause => putStrLn(s"${cause.prettyPrint}") as ExitCode.failure)
}

object Looping extends App {
  import zio.console._

  def repeat[R, E, A](n: Int)(effect: ZIO[R, E, A]): ZIO[R, E, A] = {
    if (n <= 1) effect
    else effect *> repeat(n - 1)(effect)
  }

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    repeat(3)(putStrLn("Alex was here")) as ExitCode.success
}

object PromptName extends App {
  import zio.console._

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
  //    putStrLn("What's your name") *>
  //  getStrLn.flatMap(name => putStrLn(s"Hello, ${name}")).fold(_ => ExitCode.failure,_=> ExitCode.success)
  // if you have a lot of flatMaps, then it might become messy
    (for {
      _ <- putStrLn("What's your name")
      name <- getStrLn
      _ <- putStrLn(s"Hello, ${name}")
    } yield ExitCode.success) orElse ZIO.succeed(ExitCode.failure)
}

object NumberGuesser extends  App{
  import zio.console._
  import zio.random._

  def analyzeGuess(value:Int, guess:String) =
    if(value.toString == guess) putStrLn("Congrats, you guessed the number!")
    else putStrLn("Ups, you had bad luck!")

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    (
      for {
        random <- nextIntBetween(0,3)
        _ <- putStrLn("Please guess a number from 0 to 3")
        guess <- getStrLn
        _ <- analyzeGuess(random, guess)
      } yield ExitCode.success)  orElse ZIO.succeed(ExitCode.failure)
}
