package io.pager.subscription

import io.pager.client.telegram.ChatId
import io.pager.log.Logger
import io.pager.subscription.Repository.{ Name, Version }
import io.pager.subscription.chat.ChatStorage
import io.pager.subscription.repository.RepositoryVersionStorage
import zio.{ Task, ZIO }

private[subscription] final case class Live(
  logger: Logger.Service,
  chatStorage: ChatStorage.Service,
  repositoryVersionStorage: RepositoryVersionStorage.Service
) extends SubscriptionLogic.Service {
  override def subscribe(chatId: ChatId, name: Name): Task[Unit] =
    logger.info(s"$chatId subscribed to $name") *>
      chatStorage.subscribe(chatId, name) *>
      repositoryVersionStorage.addRepository(name)

  override def unsubscribe(chatId: ChatId, name: Name): Task[Unit] =
    logger.info(s"Chat $chatId unsubscribed from $name") *>
      chatStorage.unsubscribe(chatId, name)

  override def listSubscriptions(chatId: ChatId): Task[Set[Name]] =
    logger.info(s"Chat $chatId requested subscriptions") *>
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
