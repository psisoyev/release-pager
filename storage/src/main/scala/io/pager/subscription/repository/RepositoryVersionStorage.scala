package io.pager.subscription.repository

import doobie.util.transactor.Transactor
import io.pager.subscription.Repository.{ Name, Version }
import io.pager.subscription.repository.RepositoryVersionStorage.RepositoryVersionMap
import zio._

trait RepositoryVersionStorage {
  def addRepository(name: Name): UIO[Unit]
  def updateVersion(name: Name, version: Version): UIO[Unit]
  def deleteRepository(name: Name): UIO[Unit]

  def listRepositories: UIO[RepositoryVersionMap]
}

object RepositoryVersionStorage {
  type RepositoryVersionMap = Map[Name, Option[Version]]

  val inMemory: ZLayer[Ref[RepositoryVersionMap], Nothing, RepositoryVersionStorage] = ZLayer {
    ZIO.service[Ref[RepositoryVersionMap]].map(InMemory)
  }

  val doobie: ZLayer[Transactor[Task], Nothing, RepositoryVersionStorage] = ZLayer {
    ZIO.service[Transactor[Task]].map(Doobie)
  }
}
