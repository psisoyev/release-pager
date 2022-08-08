package io.pager.subscription.repository

import doobie.implicits._
import doobie.util.transactor.Transactor
import doobie.{ Query0, Update0 }
import io.pager.subscription.Repository.{ Name, Version }
import io.pager.subscription.repository.RepositoryVersionStorage.RepositoryVersionMap
import zio.interop.catz._
import zio.{ Task, UIO }

private[repository] final case class Doobie(xa: Transactor[Task]) extends RepositoryVersionStorage {
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

private object SQL {
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
