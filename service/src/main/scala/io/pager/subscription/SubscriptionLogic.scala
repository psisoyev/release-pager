package io.pager.subscription

import io.pager.client.telegram.ChatId
import io.pager.logging.Logger
import io.pager.subscription.RepositoryStatus.Version
import zio.{Task, UIO, ZIO}

trait SubscriptionLogic {
  val subscription: SubscriptionLogic.Service
}

object SubscriptionLogic {
  trait Service {
    def subscribe(chatId: ChatId, repositoryName: RepositoryName): UIO[Unit]
    def unsubscribe(chatId: ChatId, repositoryName: RepositoryName): UIO[Unit]
    def listSubscriptions(chatId: ChatId): Task[Set[RepositoryName]]
    def listRepositories: UIO[Map[RepositoryName, Option[Version]]]
    def listSubscribers(repositoryName: RepositoryName): UIO[Set[ChatId]]
    def updateVersions(updatedVersions: Map[RepositoryName, RepositoryStatus.Version]): UIO[Unit]
  }

  trait Live extends SubscriptionLogic {
    def logger: Logger.Service
    def chatStorage: ChatStorage.Service
    def repositoryVersionStorage: RepositoryVersionStorage.Service

    override val subscription: Service = new Service {
      override def subscribe(chatId: ChatId, repositoryName: RepositoryName): UIO[Unit] =
        logger.info(s"$chatId subscribed to $repositoryName") *>
          chatStorage.subscribe(chatId, repositoryName)

      override def unsubscribe(chatId: ChatId, repositoryName: RepositoryName): UIO[Unit] =
        logger.info(s"$chatId unsubscribed from $repositoryName") *>
          chatStorage.unsubscribe(chatId, repositoryName)

      override def listSubscriptions(chatId: ChatId): Task[Set[RepositoryName]] =
        logger.info(s"$chatId requested subscriptions") *>
          chatStorage.listSubscriptions(chatId)

      override def listRepositories: UIO[Map[RepositoryName, Option[Version]]] =
        logger.info(s"Listing repositories") *>
          repositoryVersionStorage.listRepositories

      override def listSubscribers(repositoryName: RepositoryName): UIO[Set[ChatId]] =
        logger.info(s"Listing repository $repositoryName subscribers") *>
          chatStorage.listSubscribers(repositoryName)

      override def updateVersions(updatedVersions: Map[RepositoryName, Version]): UIO[Unit] =
        ZIO
          .foreach(updatedVersions) {
            case (name, version) =>
              logger.info(s"Updating repository $name version to $version") *>
                repositoryVersionStorage.updateVersion(name, version)
          }
          .unit
    }
  }
}
