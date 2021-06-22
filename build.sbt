import sbt._
import Settings._

lazy val domain = project
  .settings(commonSettings)

lazy val storage = project
  .settings(commonSettings)
  .settings(libraryDependencies ++= storageDependencies)
  .dependsOn(domain)

lazy val service = project
  .settings(commonSettings)
  .settings(libraryDependencies ++= serviceDependencies)
  .dependsOn(storage)

lazy val backend = project
  .settings(commonSettings)
  .settings(libraryDependencies ++= backendDependencies)
  .dependsOn(service)

lazy val `release-pager` = Project("release-pager", file("."))
  .settings(commonSettings)
  .settings(organization := "psisoyev.io")
  .settings(moduleName := "release-pager")
  .settings(name := "release-pager")
  .aggregate(
    domain,
    storage,
    service,
    backend
  )