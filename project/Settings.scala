import Dependencies._
import org.scalafmt.sbt.ScalafmtPlugin.autoImport.scalafmtOnCompile
import sbt.Keys.{scalacOptions, _}
import sbt._
import sbt.util.Level
import wartremover._

object Settings {
  val warts = Warts.allBut(
    Wart.Any,
    Wart.TraversableOps,
    Wart.StringPlusAny,
    Wart.Nothing,
    Wart.Overloading,
    Wart.JavaSerializable,
    Wart.PublicInference,
    Wart.Serializable,
    Wart.DefaultArguments)

  val commonSettings = {
    Seq(
      scalaVersion := "2.13.3",
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
      testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework")),

      cancelable in Global := true,
      fork in Global := true, // https://github.com/sbt/sbt/issues/2274
      resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
    )
  }

  val storageDependencies = List(zio, zioCats) ++ doobie
  val serviceDependencies = List(zioCats, zioMacros, zioTest, zioTestSbt, fs2Core, canoe, slf4j) ++ circe

  val backendDependencies = List(flyway, pureconfig, h2)

  val higherKinds = addCompilerPlugin("org.typelevel" %% "kind-projector" % Version.kindProjector)
}