import sbt._

object Dependencies {
  val zio = "dev.zio" %% "zio" % Version.zio
  val zioCats = ("dev.zio" %% "zio-interop-cats" % Version.zioCats).excludeAll(ExclusionRule("dev.zio"))
  val zioMacros = "dev.zio" %% "zio-macros" % Version.zio
  val zioTest = "dev.zio" %% "zio-test"     % Version.zio % "test"
  val zioTestSbt = "dev.zio" %% "zio-test-sbt" % Version.zio % "test"

  val fs2Core = "co.fs2" %% "fs2-core" % Version.fs2Core

  val doobieCore = "org.tpolecat" %% "doobie-core" % Version.doobie
  val doobieH2 = "org.tpolecat" %% "doobie-h2" % Version.doobie
  val doobieHikari = "org.tpolecat" %% "doobie-hikari" % Version.doobie
  val doobie = List(doobieCore, doobieH2, doobieHikari)

  val flyway = "org.flywaydb" % "flyway-core" % Version.flyway
  val h2 = "com.h2database" % "h2" % Version.h2

  val circeGeneric = "io.circe" %% "circe-generic" % Version.circe
  val circeCore = "io.circe" %% "circe-core" % Version.circe
  val circeParser = "io.circe" %% "circe-parser" % Version.circe
  val circeExtras = "io.circe" %% "circe-generic-extras" % Version.circe
  val circe = List(circeGeneric, circeCore, circeParser, circeExtras)

  val slf4j = "org.slf4j" % "slf4j-simple" % Version.slf4j

  val canoe = "org.augustjune" %% "canoe" % Version.canoe

  val pureconfig = "com.github.pureconfig" %% "pureconfig" % Version.pureconfig
}

object Version {
  val zio = "1.0.5"
  val zioCats = "2.4.0.0"
  val slf4j = "1.7.30"
  val fs2Core = "2.5.4"
  val kindProjector = "0.10.3"
  val canoe = "0.5.1"
  val circe = "0.13.0"
  val doobie = "0.12.1"
  val flyway = "7.7.1"
  val h2 = "1.4.200"
  val pureconfig = "0.14.1"
}