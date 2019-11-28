package io.pager.subscription

import io.pager.subscription.RepositoryStatus.Version
import zio.{ Ref, UIO }

trait RepositoryVersionStorage {
  val repositoryVersionStorage: RepositoryVersionStorage.Service
}

object RepositoryVersionStorage {
  type SubscriberMap = Map[RepositoryName, Option[Version]]

  trait Service {
    def listRepositories: UIO[SubscriberMap]
    def addRepository(repositoryName: RepositoryName): UIO[Unit]
    def deleteRepository(repositoryName: RepositoryName): UIO[Unit]
    def updateVersion(repositoryName: RepositoryName, version: Version): UIO[Unit]
  }

  trait InMemory extends RepositoryVersionStorage {
    def subscribers: Ref[SubscriberMap]

    val repositoryVersionStorage: Service = new Service {
      override def listRepositories: UIO[SubscriberMap] = subscribers.get

      override def addRepository(repositoryName: RepositoryName): UIO[Unit] =
        subscribers.update { current =>
          current + (repositoryName -> None)
        }.unit

      override def deleteRepository(repositoryName: RepositoryName): UIO[Unit] =
        subscribers
          .update(_ - repositoryName)
          .unit

      override def updateVersion(repositoryName: RepositoryName, version: Version): UIO[Unit] =
        subscribers.update { current =>
          current + (repositoryName -> Some(version))
        }.unit
    }
  }

  object Test {
    def instance: UIO[RepositoryVersionStorage.Service] =
      Ref.make(Map.empty[RepositoryName, Option[Version]]).map { subscriberMap =>
        new InMemory {
          override def subscribers: Ref[SubscriberMap] = subscriberMap
        }.repositoryVersionStorage
      }
  }
}
