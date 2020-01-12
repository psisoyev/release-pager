package io.pager.subscription

import io.pager.client.telegram.ChatId
import io.pager.logging.Logger
import io.pager.subscription.Repository.{ Name, Version }
import zio.macros.annotation.mockable
import zio.{ RIO, Task, ZIO }

@mockable
trait SubscriptionLogic {
  val subscriptionLogic: SubscriptionLogic.Service[Any]
}

object SubscriptionLogic {
  trait Service[R] {
    def subscribe(chatId: ChatId, name: Name): RIO[R, Unit]
    def unsubscribe(chatId: ChatId, name: Name): RIO[R, Unit]
    def listSubscriptions(chatId: ChatId): RIO[R, Set[Name]]
    def listRepositories: RIO[R, Map[Name, Option[Version]]]
    def listSubscribers(name: Name): RIO[R, Set[ChatId]]
    def updateVersions(updatedVersions: Map[Name, Version]): RIO[R, Unit]
  }

  trait Live extends SubscriptionLogic {
    def logger: Logger.Service
    def chatStorage: ChatStorage.Service
    def repositoryVersionStorage: RepositoryVersionStorage.Service

    override val subscriptionLogic: Service[Any] = new Service[Any] {
      override def subscribe(chatId: ChatId, name: Name): Task[Unit] =
        logger.info(s"$chatId subscribed to ${name.value}") *>
          chatStorage.subscribe(chatId, name) *>
          repositoryVersionStorage.addRepository(name)

      override def unsubscribe(chatId: ChatId, name: Name): Task[Unit] =
        logger.info(s"Chat ${chatId.value} unsubscribed from ${name.value}") *>
          chatStorage.unsubscribe(chatId, name)

      override def listSubscriptions(chatId: ChatId): Task[Set[Name]] =
        logger.info(s"Chat ${chatId.value} requested subscriptions") *>
          chatStorage.listSubscriptions(chatId)

      override def listRepositories: Task[Map[Name, Option[Version]]] =
        logger.info(s"Listing repositories") *>
          repositoryVersionStorage.listRepositories

      override def listSubscribers(name: Name): Task[Set[ChatId]] =
        logger.info(s"Listing repository ${name.value} subscribers") *>
          chatStorage.listSubscribers(name)

      override def updateVersions(updatedVersions: Map[Name, Version]): Task[Unit] =
        ZIO
          .foreach(updatedVersions) {
            case (name, version) =>
              logger.info(s"Updating repository ${name.value} version to $version") *>
                repositoryVersionStorage.updateVersion(name, version)
          }
          .unit
    }
  }

  object Live {
    def make(
      logger: Logger.Service,
      chatStorageService: ChatStorage.Service,
      repositoryVersionStorageService: RepositoryVersionStorage.Service
    ): SubscriptionLogic.Service[Any] =
      new SubscriptionLogic.Live {
        override def logger: Logger.Service                                     = Logger.Test
        override def chatStorage: ChatStorage.Service                           = chatStorageService
        override def repositoryVersionStorage: RepositoryVersionStorage.Service = repositoryVersionStorageService
      }.subscriptionLogic
  }
}
