import sbt._

object Dependencies {
  val zio        = "dev.zio"  %% "zio"              % Version.zio
  val zioCats    = ("dev.zio" %% "zio-interop-cats" % Version.zioCats).excludeAll(ExclusionRule("dev.zio"))
  val zioMacros  = "dev.zio"  %% "zio-macros"       % Version.zio
  val zioTest    = "dev.zio"  %% "zio-test"         % Version.zio % "test"
  val zioTestSbt = "dev.zio"  %% "zio-test-sbt"     % Version.zio % "test"

  val fs2Core = "co.fs2" %% "fs2-core" % Version.fs2Core

  val doobieCore   = "org.tpolecat" %% "doobie-core"   % Version.doobie
  val doobieH2     = "org.tpolecat" %% "doobie-h2"     % Version.doobie
  val doobieHikari = "org.tpolecat" %% "doobie-hikari" % Version.doobie
  val doobie       = List(doobieCore, doobieH2, doobieHikari)

  val flyway = "org.flywaydb"   % "flyway-core" % Version.flyway
  val h2     = "com.h2database" % "h2"          % Version.h2

  val circeGeneric = "io.circe" %% "circe-generic"        % Version.circe
  val circeCore    = "io.circe" %% "circe-core"           % Version.circe
  val circeParser  = "io.circe" %% "circe-parser"         % Version.circe
  val circeExtras  = "io.circe" %% "circe-generic-extras" % Version.circe
  val circe        = List(circeGeneric, circeCore, circeParser, circeExtras)

  val slf4j = "org.slf4j" % "slf4j-simple" % Version.slf4j

  val canoe = "org.augustjune" %% "canoe" % Version.canoe

  val pureconfig = "com.github.pureconfig" %% "pureconfig" % Version.pureconfig
}

object Version {
  val zio        = "1.0.13"
  val zioCats    = "2.5.1.0"
  val slf4j      = "1.7.32"
  val fs2Core    = "2.5.10"
  val canoe      = "0.5.1"
  val circe      = "0.14.1"
  val doobie     = "0.13.4"
  val flyway     = "8.2.1"
  val h2         = "2.0.202"
  val pureconfig = "0.17.1"
}
