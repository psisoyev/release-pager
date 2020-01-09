import Dependencies._
import org.scalafmt.sbt.ScalafmtPlugin.autoImport.scalafmtOnCompile
import sbt.Keys.{scalacOptions, _}
import sbt._
import sbt.util.Level
import wartremover._

object Settings {
  val warts = Warts.allBut(Wart.Any, Wart.TraversableOps, Wart.StringPlusAny, Wart.Nothing, Wart.Overloading, Wart.JavaSerializable, Wart.PublicInference, Wart.Serializable)

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
      scalafmtOnCompile := true,
      wartremoverErrors in (Compile, compile) ++= warts,
      wartremoverErrors in (Test, compile) ++= warts,
      testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework"))
    )
  }

  val storageDependencies = List(zio) ++ doobie
  val serviceDependencies = List(zio, zioCats, zioTest, zioTestSbt, zioMacro, zioMacroTest, fs2Core, slf4j, canoe, scalaTest) ++ circe

  val higherKinds = addCompilerPlugin("org.typelevel" %% "kind-projector" % Version.kindProjector)
}