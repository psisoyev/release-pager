package io.pager.subscription

import doobie.implicits._
import doobie.util.transactor.Transactor
import doobie.{ Query0, Update0 }
import io.pager.subscription.Repository.{ Name, Version }
import io.pager.subscription.RepositoryVersionStorage.Doobie.SQL
import io.pager.subscription.RepositoryVersionStorage.Doobie.SQL.RepositoryVersionT
import zio.{ Ref, Task, UIO }
import zio.interop.catz._

trait RepositoryVersionStorage {
  val repositoryVersionStorage: RepositoryVersionStorage.Service[Any]
}

object RepositoryVersionStorage {
  type RepositoryVersionMap = Map[Name, Option[Version]]

  trait Service[R] {
    def addRepository(name: Name): UIO[Unit]
    def updateVersion(name: Name, version: Version): UIO[Unit]
    def deleteRepository(name: Name): UIO[Unit]

    def listRepositories: UIO[RepositoryVersionMap]
  }

  trait InMemory extends RepositoryVersionStorage {
    def versions: Ref[RepositoryVersionMap]

    val repositoryVersionStorage: Service[Any] = new Service[Any] {
      override def listRepositories: UIO[RepositoryVersionMap] = versions.get

      override def addRepository(name: Name): UIO[Unit] =
        versions
          .update(_ + (name -> None))
          .unit

      override def deleteRepository(name: Name): UIO[Unit] =
        versions
          .update(_ - name)
          .unit

      override def updateVersion(name: Name, version: Version): UIO[Unit] =
        versions
          .update(_ + (name -> Some(version)))
          .unit
    }
  }

  trait Doobie extends RepositoryVersionStorage {
    def xa: Transactor[Task]

    override val repositoryVersionStorage: Service[Any] = new Service[Any] {
      override def addRepository(name: Name): UIO[Unit] =
        SQL
          .create(name)
          .withUniqueGeneratedKeys[Long]("ID")
          .transact(xa)
          .unit
          .orDie

      override def updateVersion(name: Name, version: Version): UIO[Unit] =
        SQL
          .update(name, version)
          .run
          .transact(xa)
          .unit
          .orDie

      override def deleteRepository(name: Name): UIO[Unit] =
        SQL
          .delete(name)
          .run
          .transact(xa)
          .unit
          .orDie

      override def listRepositories: UIO[RepositoryVersionMap] =
        SQL
          .getAll
          .to[List]
          .transact(xa)
          .orDie
          .map(_.toMap)
    }
  }

  object Doobie {
    object SQL {
      type RepositoryVersionT = (Name, Option[Version])

      def create(name: Name): Update0 =
        sql"""INSERT INTO REPOSITORY (NAME) VALUES (${name.value})""".update

      def delete(name: Name): Update0 =
        sql"""DELETE from REPOSITORY WHERE NAME = ${name.value}""".update

      val getAll: Query0[RepositoryVersionT] =
        sql"""SELECT NAME, VERSION FROM REPOSITORY""".query[RepositoryVersionT]

      def update(name: Name, version: Version): Update0 =
        sql"""UPDATE REPOSITORY SET VERSION = ${version.value} WHERE NAME = ${name.value}""".update
    }
  }

  object Test {
    def make(state: Ref[Map[Name, Option[Version]]]): RepositoryVersionStorage.Service[Any] =
      new InMemory { def versions: Ref[RepositoryVersionMap] = state }.repositoryVersionStorage
  }
}
