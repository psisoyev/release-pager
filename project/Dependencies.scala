import sbt._

object Dependencies {
  val zio        = "dev.zio"  %% "zio"              % Version.zio
  val zioCats    = ("dev.zio" %% "zio-interop-cats" % Version.zioCats).excludeAll(ExclusionRule("org.typelevel"))
  val zioMacros  = "dev.zio"  %% "zio-macros"       % Version.zio
  val zioTest    = "dev.zio"  %% "zio-test"         % Version.zio % "test"
  val zioTestSbt = "dev.zio"  %% "zio-test-sbt"     % Version.zio % "test"

  val fs2Core = "co.fs2" %% "fs2-core" % Version.fs2Core

  val doobieCore   = "org.tpolecat" %% "doobie-core"   % Version.doobie
  val doobieH2     = "org.tpolecat" %% "doobie-h2"     % Version.doobie
  val doobieHikari = "org.tpolecat" %% "doobie-hikari" % Version.doobie
  val doobie       = List(doobieCore, doobieH2, doobieHikari)

  val http4s = List("org.http4s" %% "http4s-dsl", "org.http4s" %% "http4s-blaze-client", "org.http4s" %% "http4s-circe").map(_ % Version.http4s)

  val flyway = "org.flywaydb"   % "flyway-core" % Version.flyway
  val h2     = "com.h2database" % "h2"          % Version.h2

  val circeGeneric = "io.circe" %% "circe-generic"        % Version.circe
  val circeCore    = "io.circe" %% "circe-core"           % Version.circe
  val circeParser  = "io.circe" %% "circe-parser"         % Version.circe
  val circeExtras  = "io.circe" %% "circe-generic-extras" % Version.circe
  val circe        = List(circeGeneric, circeCore, circeParser, circeExtras)

  val slf4j = "org.slf4j" % "slf4j-simple" % Version.slf4j

  val canoe = ("org.augustjune" %% "canoe" % Version.canoe)

  val pureconfig = "com.github.pureconfig" %% "pureconfig" % Version.pureconfig
}

object Version {
  val zio        = "2.0.10"
  val zioCats    = "23.0.0.1"
  val slf4j      = "2.0.6"
  val fs2Core    = "3.6.1"
  val canoe      = "0.6.0"
  val circe      = "0.14.3"
  val doobie     = "1.0.0-RC2"
  val h2         = "2.1.212"
  val flyway     = "8.5.8"
  val pureconfig = "0.17.2"
  val http4s     = "0.23.9"
}
