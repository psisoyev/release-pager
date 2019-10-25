import sbt._
import Settings._

lazy val domain = project
  .settings(commonSettings)
  .settings(libraryDependencies ++= domainDependencies)

lazy val service = project
  .settings(commonSettings)
  .settings(libraryDependencies ++= serviceDependencies)
  .settings(libraryDependencies ++= storageDependencies)
  .settings(higherKinds)
  .dependsOn(domain)

lazy val backend = project
  .settings(commonSettings)
  .dependsOn(service)

lazy val `release-pager` = Project("release-pager", file("."))
  .settings(commonSettings)
  .settings(organization := "psisoyev.io")
  .settings(moduleName := "release-pager")
  .settings(name := "release-pager")
  .aggregate(
    domain,
    service,
    backend
  )