package io.pager.subscription

import io.pager.client.telegram.ChatId
import io.pager.log.Logger
import io.pager.subscription.Repository.{ Name, Version }
import io.pager.subscription.chat.ChatStorage
import io.pager.subscription.repository.RepositoryVersionStorage
import zio._

trait SubscriptionLogic {
  def subscribe(chatId: ChatId, name: Name): Task[Unit]

  def unsubscribe(chatId: ChatId, name: Name): Task[Unit]

  def listSubscriptions(chatId: ChatId): Task[Set[Name]]

  def listRepositories: Task[Map[Name, Option[Version]]]

  def listSubscribers(name: Name): Task[Set[ChatId]]

  def updateVersions(updatedVersions: Map[Name, Version]): Task[Unit]
}

object SubscriptionLogic {
  type LiveDeps = Logger with ChatStorage with RepositoryVersionStorage
  val live: URLayer[LiveDeps, SubscriptionLogic] = ZLayer {
    for {
      logger                   <- ZIO.service[Logger]
      chatStorage              <- ZIO.service[ChatStorage]
      repositoryVersionStorage <- ZIO.service[RepositoryVersionStorage]
    } yield Live(logger, chatStorage, repositoryVersionStorage)
  }

  val dummy: ULayer[SubscriptionLogic] = ZLayer.succeed {
    new SubscriptionLogic {
      override def subscribe(chatId: ChatId, name: Name): Task[Unit] = ZIO.unit

      override def unsubscribe(chatId: ChatId, name: Name): Task[Unit] = ZIO.unit

      override def listSubscriptions(chatId: ChatId): Task[Set[Name]] = ZIO.succeed(Set.empty)

      override def listRepositories: Task[Map[Name, Option[Version]]] = ZIO.succeed(Map.empty)

      override def listSubscribers(name: Name): Task[Set[ChatId]] = ZIO.succeed(Set.empty)

      override def updateVersions(updatedVersions: Map[Name, Version]): Task[Unit] = ZIO.unit
    }
  }
}
