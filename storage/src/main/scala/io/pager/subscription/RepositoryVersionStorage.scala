package io.pager.subscription

import io.pager.subscription.Repository.{ Name, Version }
import zio.{ Ref, UIO }

trait RepositoryVersionStorage {
  val repositoryVersionStorage: RepositoryVersionStorage.Service
}

object RepositoryVersionStorage {
  type SubscriberMap = Map[Name, Option[Version]]

  trait Service {
    def listRepositories: UIO[SubscriberMap]
    def addRepository(name: Name): UIO[Unit]
    def deleteRepository(name: Name): UIO[Unit]
    def updateVersion(name: Name, version: Version): UIO[Unit]
  }

  trait InMemory extends RepositoryVersionStorage {
    def subscribers: Ref[SubscriberMap]

    val repositoryVersionStorage: Service = new Service {
      override def listRepositories: UIO[SubscriberMap] = subscribers.get

      override def addRepository(name: Name): UIO[Unit] =
        subscribers.update { current =>
          current + (name -> None)
        }.unit

      override def deleteRepository(name: Name): UIO[Unit] =
        subscribers
          .update(_ - name)
          .unit

      override def updateVersion(name: Name, version: Version): UIO[Unit] =
        subscribers
          .update(_ + (name -> Some(version)))
          .unit
    }
  }

  object Test {
    def instance: UIO[RepositoryVersionStorage.Service] =
      Ref
        .make(Map.empty[Name, Option[Version]])
        .map { subscriberMap =>
          new InMemory { def subscribers: Ref[SubscriberMap] = subscriberMap }
        }
        .map(_.repositoryVersionStorage)
  }
}
