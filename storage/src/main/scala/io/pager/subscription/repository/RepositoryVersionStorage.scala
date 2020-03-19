package io.pager.subscription.repository

import doobie.util.transactor.Transactor
import io.pager.subscription.Repository.{ Name, Version }
import zio._

object RepositoryVersionStorage {
  type RepositoryVersionStorage = Has[Service]

  type RepositoryVersionMap = Map[Name, Option[Version]]

  trait Service {
    def addRepository(name: Name): UIO[Unit]
    def updateVersion(name: Name, version: Version): UIO[Unit]
    def deleteRepository(name: Name): UIO[Unit]

    def listRepositories: UIO[RepositoryVersionMap]
  }

  val inMemory: ZLayer[Has[Ref[RepositoryVersionMap]], Nothing, Has[Service]] =
    ZLayer.fromService[Ref[RepositoryVersionMap], Service] { subscriptions =>
      InMemory(subscriptions)
    }

  val doobie: ZLayer[Has[Transactor[Task]], Nothing, Has[Service]] =
    ZLayer.fromService[Transactor[Task], Service] { xa: Transactor[Task] =>
      Doobie(xa)
    }
}
