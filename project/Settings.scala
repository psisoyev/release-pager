import Dependencies._
import sbt.Keys.{scalacOptions, _}
import sbt._
import sbt.util.Level

object Settings {
  val commonSettings = {
    Seq(
      scalaVersion := "2.13.1",
      scalacOptions := Seq(
        "-Ymacro-annotations",
        "-deprecation",
        "-encoding", "utf-8",
        "-explaintypes",
        "-feature",
        "-unchecked",
        "-language:postfixOps",
        "-language:higherKinds",
        "-language:implicitConversions",
        "-Xcheckinit",
        "-Xfatal-warnings"
      ),
      javacOptions ++= Seq("-g", "-source", "1.8", "-target", "1.8", "-encoding", "UTF-8"),
      logLevel := Level.Info,
      version := (version in ThisBuild).value,
      publishArtifact in (Compile, packageDoc) := false
    )
  }

  val storageDependencies = doobie
  val serviceDependencies = List(zio, zioCats, fs2Core, slf4j, canoe, scalaTest) ++ circe
  val domainDependencies = List(newtype)
  val backendDependencies = List(pureconfig)
}