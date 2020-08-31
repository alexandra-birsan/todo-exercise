lazy val catsVersion   = "2.0.0"
lazy val circeVersion  = "0.12.2"
lazy val http4sVersion = "0.21.7"
lazy val tapirVersion  = "0.16.15"
lazy val zioVersion    = "1.0.0"

name := "todo"
version := "0.1"
scalaVersion := "2.13.2"
resolvers += Resolver.sonatypeRepo("releases")
libraryDependencies ++= Seq(
  "dev.zio" %% "zio-interop-cats" % "2.1.4.0",
  "dev.zio" %% "zio" % zioVersion,
  "org.tpolecat" %% "doobie-core" % "0.8.8",
  "org.xerial" % "sqlite-jdbc" % "3.28.0",
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-core" % tapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % tapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % tapirVersion,
  "org.http4s" %% "http4s-blaze-server" % http4sVersion,
  "com.pauldijou" %% "jwt-circe" % "4.2.0",
  "org.scalatest" %% "scalatest" % "3.0.8" % Test,
  "dev.zio" %% "zio-test" % zioVersion % Test,
  "dev.zio" %% "zio-test-sbt" % zioVersion % Test
)
addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.10.3")
testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework"))
