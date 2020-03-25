package io.pager.subscription

import io.pager.client.telegram.ChatId
import io.pager.log.Logger
import io.pager.log.Logger.Logger
import io.pager.subscription.chat.ChatStorage.ChatStorage
import io.pager.subscription.Repository.{ Name, Version }
import io.pager.subscription.repository.RepositoryVersionStorage.RepositoryVersionStorage
import io.pager.subscription.chat.ChatStorage
import io.pager.subscription.repository.RepositoryVersionStorage
import zio._
import zio.macros.accessible

@accessible
object SubscriptionLogic {
  type SubscriptionLogic = Has[Service]

  trait Service {
    def subscribe(chatId: ChatId, name: Name): Task[Unit]
    def unsubscribe(chatId: ChatId, name: Name): Task[Unit]

    def listSubscriptions(chatId: ChatId): Task[Set[Name]]
    def listRepositories: Task[Map[Name, Option[Version]]]
    def listSubscribers(name: Name): Task[Set[ChatId]]

    def updateVersions(updatedVersions: Map[Name, Version]): Task[Unit]
  }

  type LiveDeps = Logger with ChatStorage with RepositoryVersionStorage
  def live: URLayer[LiveDeps, Has[Service]] =
    ZLayer.fromServices[Logger.Service, ChatStorage.Service, RepositoryVersionStorage.Service, Service] {
      (logger, chatStorage, repositoryVersionStorage) =>
        Live(logger, chatStorage, repositoryVersionStorage)
    }
}
