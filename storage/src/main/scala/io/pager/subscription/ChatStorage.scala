package io.pager.subscription

import io.pager.client.telegram.ChatId
import zio.{ Ref, UIO, ZIO }

trait ChatStorage {
  val chatStorage: ChatStorage.Service
}

object ChatStorage {
  type SubscriptionMap = Map[ChatId, Set[RepositoryName]]

  trait Service {
    def listSubscriptions(chatId: ChatId): UIO[Set[RepositoryName]]
    def listSubscribers(repositoryName: RepositoryName): UIO[Set[ChatId]]
    def subscribe(chatId: ChatId, repositoryName: RepositoryName): UIO[Unit]
    def unsubscribe(chatId: ChatId, repositoryName: RepositoryName): UIO[Unit]
  }

  trait InMemory extends ChatStorage {
    def subscriptions: Ref[SubscriptionMap]

    val chatStorage: Service = new Service {
      override def listSubscriptions(chatId: ChatId): UIO[Set[RepositoryName]] =
        subscriptions.get.map(_.getOrElse(chatId, Set.empty))

      override def listSubscribers(repositoryName: RepositoryName): UIO[Set[ChatId]] =
        subscriptions.get
          .map(_.collect { case (chatId, repos) if repos.contains(repositoryName) => chatId }.toSet)

      override def subscribe(chatId: ChatId, repositoryName: RepositoryName): UIO[Unit] =
        updateSubscriptions(chatId)(_ + repositoryName).unit

      override def unsubscribe(chatId: ChatId, repositoryName: RepositoryName): UIO[Unit] =
        updateSubscriptions(chatId)(_.filterNot(_ == repositoryName)).unit

      private def updateSubscriptions(chatId: ChatId)(f: Set[RepositoryName] => Set[RepositoryName]): UIO[SubscriptionMap] =
        subscriptions.update { current =>
          val subscriptions = current.getOrElse(chatId, Set.empty)
          current + (chatId -> f(subscriptions))
        }
    }
  }

  object Test {
    def instance: UIO[Service] =
      Ref
        .make(Map.empty[ChatId, Set[RepositoryName]])
        .map { subscriptionMap =>
          new ChatStorage.InMemory {
            override def subscriptions: Ref[SubscriptionMap] = subscriptionMap
          }.chatStorage
        }
  }
}
