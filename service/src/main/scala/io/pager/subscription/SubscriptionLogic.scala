package io.pager.subscription

import io.pager.client.telegram.ChatId
import io.pager.log.Logger
import io.pager.log.Logger.Logger
import io.pager.subscription.Repository.{ Name, Version }
import zio.{ Has, RIO, Task, ZIO, ZLayer }
import io.pager.subscription.ChatStorage.ChatStorage
import io.pager.subscription.RepositoryVersionStorage.RepositoryVersionStorage

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
  val live: ZLayer[LiveDeps, Nothing, Has[Service]] =
    ZLayer.fromServices[Logger.Service, ChatStorage.Service, RepositoryVersionStorage.Service, Service] {
      (logger, chatStorage, repositoryVersionStorage) =>
        Live(logger, chatStorage, repositoryVersionStorage)
    }

  def subscribe(chatId: ChatId, name: Name): RIO[SubscriptionLogic, Unit]   = ZIO.accessM[SubscriptionLogic](_.get.subscribe(chatId, name))
  def unsubscribe(chatId: ChatId, name: Name): RIO[SubscriptionLogic, Unit] = ZIO.accessM[SubscriptionLogic](_.get.unsubscribe(chatId, name))

  def listSubscriptions(chatId: ChatId): RIO[SubscriptionLogic, Set[Name]] = ZIO.accessM[SubscriptionLogic](_.get.listSubscriptions(chatId))
  def listRepositories: RIO[SubscriptionLogic, Map[Name, Option[Version]]] = ZIO.accessM[SubscriptionLogic](_.get.listRepositories)
  def listSubscribers(name: Name): RIO[SubscriptionLogic, Set[ChatId]]     = ZIO.accessM[SubscriptionLogic](_.get.listSubscribers(name))

  def updateVersions(updatedVersions: Map[Name, Version]): RIO[SubscriptionLogic, Unit] =
    ZIO.accessM[SubscriptionLogic](_.get.updateVersions(updatedVersions))
}
