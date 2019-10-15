import sbt._

object Dependencies {
  val zio = "dev.zio" %% "zio" % Version.zio
  val zioCats = "dev.zio" %% "zio-interop-cats" % Version.zioCats
  val slf4j = "org.slf4j" % "slf4j-simple" % Version.slf4j
}

object Version {
  val zio = "1.0.0-RC14"
  val zioCats = "2.0.0.0-RC5"
  val slf4j = "1.7.28"
}