package io.pager.subscription

import io.pager.client.telegram.ChatId
import io.pager.logging.Logger
import io.pager.subscription.Repository.{ Name, Version }
import zio.{ Task, ZIO }

trait SubscriptionLogic {
  val subscription: SubscriptionLogic.Service
}

object SubscriptionLogic {
  trait Service {
    def subscribe(chatId: ChatId, name: Name): Task[Unit]
    def unsubscribe(chatId: ChatId, name: Name): Task[Unit]
    def listSubscriptions(chatId: ChatId): Task[Set[Name]]
    def listRepositories: Task[Map[Name, Option[Version]]]
    def listSubscribers(name: Name): Task[Set[ChatId]]
    def updateVersions(updatedVersions: Map[Name, Version]): Task[Unit]
  }

  trait Live extends SubscriptionLogic {
    def logger: Logger.Service
    def chatStorage: ChatStorage.Service
    def repositoryVersionStorage: RepositoryVersionStorage.Service

    override val subscription: Service = new Service {
      override def subscribe(chatId: ChatId, name: Name): Task[Unit] =
        logger.info(s"$chatId subscribed to $name") *>
          chatStorage.subscribe(chatId, name)

      override def unsubscribe(chatId: ChatId, name: Name): Task[Unit] =
        logger.info(s"$chatId unsubscribed from $name") *>
          chatStorage.unsubscribe(chatId, name)

      override def listSubscriptions(chatId: ChatId): Task[Set[Name]] =
        logger.info(s"$chatId requested subscriptions") *>
          chatStorage.listSubscriptions(chatId)

      override def listRepositories: Task[Map[Name, Option[Version]]] =
        logger.info(s"Listing repositories") *>
          repositoryVersionStorage.listRepositories

      override def listSubscribers(name: Name): Task[Set[ChatId]] =
        logger.info(s"Listing repository $name subscribers") *>
          chatStorage.listSubscribers(name)

      override def updateVersions(updatedVersions: Map[Name, Version]): Task[Unit] =
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
