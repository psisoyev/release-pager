import Dependencies._
import org.scalafmt.sbt.ScalafmtPlugin.autoImport.scalafmtOnCompile
import sbt.Keys._
import sbt._
import sbt.util.Level
import wartremover.WartRemover.autoImport.wartremoverErrors
import wartremover.{ Wart, Warts }

object Settings {
  val warts = Warts.allBut(
    Wart.Any,
    Wart.StringPlusAny,
    Wart.IterableOps,
    Wart.Nothing,
    Wart.Overloading,
    Wart.JavaSerializable,
    Wart.PublicInference,
    Wart.Serializable,
    Wart.DefaultArguments,
    Wart.GlobalExecutionContext
  )

  val commonSettings =
    Seq(
      scalaVersion         := "2.13.10",
      scalacOptions        := Seq(
        "-Ymacro-annotations",
        "-deprecation",
        "-encoding",
        "utf-8",
        "-explaintypes",
        "-feature",
        "-unchecked",
        "-language:postfixOps",
        "-language:higherKinds",
        "-language:implicitConversions",
        "-Xcheckinit",
        "-Xfatal-warnings"
      ),
      logLevel             := Level.Info,
      scalafmtOnCompile    := true,
      Compile / wartremoverErrors ++= warts,
      Test / wartremoverErrors ++= warts,
      testFrameworks       := Seq(new TestFramework("zio.test.sbt.ZTestFramework")),
      cancelable in Global := true,
      fork in Global       := true, // https://github.com/sbt/sbt/issues/2274
      resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
    )

  val storageDependencies = List(zio, zioCats) ++ doobie
  val serviceDependencies = List(zioCats, zioTest, zioTestSbt, fs2Core, canoe, slf4j) ++ circe

  val backendDependencies = List(flyway, pureconfig, h2)
}
