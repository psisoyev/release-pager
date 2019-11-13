import sbt._

object Dependencies {
  val zio = "dev.zio" %% "zio" % Version.zio
  val zioCats = "dev.zio" %% "zio-interop-cats" % Version.zioCats
  val zioTest = "dev.zio" %% "zio-test"     % Version.zio % "test"
  val zioTestSbt = "dev.zio" %% "zio-test-sbt" % Version.zio % "test"

  val fs2Core = "co.fs2" %% "fs2-core" % Version.fs2Core

  val doobieCore = "org.tpolecat" %% "doobie-core" % Version.doobie
  val doobieH2 = "org.tpolecat" %% "doobie-h2" % Version.doobie
  val doobieHikari = "org.tpolecat" %% "doobie-hikari" % Version.doobie
  val doobie = List(doobieCore, doobieH2, doobieHikari)

  val circeGeneric = "io.circe" %% "circe-generic" % Version.circe
  val circeCore = "io.circe" %% "circe-core" % Version.circe
  val circeParser = "io.circe" %% "circe-parser" % Version.circe
  val circe = List(circeGeneric, circeCore, circeParser)

  val slf4j = "org.slf4j" % "slf4j-simple" % Version.slf4j

  val canoe = "org.augustjune" %% "canoe" % Version.canoe

  val pureconfig = "com.github.pureconfig" %% "pureconfig" % Version.pureconfig

  val scalaTest = "org.scalatest" %% "scalatest" % Version.scalaTest % Test
}

object Version {
  val zio = "1.0.0-RC16"
  val zioCats = "2.0.0.0-RC7"
  val fs2Core = "2.0.1"
  val slf4j = "1.7.28"
  val kindProjector = "0.10.3"
  val canoe = "0.2.0"
  val http4s = "0.21.0-M5"
  val circe = "0.12.3"
  val pureconfig = "0.11.1"
  val scalaTest = "3.0.8"
  val doobie = "0.8.4"
}