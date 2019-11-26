package io.pager.subscription

import io.pager.api.telegram.ChatId
import zio.{ Ref, Task, UIO }

trait Subscription {
  val subscription: Subscription.Service
}

object Subscription {
  trait Service {
    def subscribe(chatId: ChatId, repositoryName: RepositoryName): UIO[Unit]
    def unsubscribe(chatId: ChatId, repositoryName: RepositoryName): UIO[Unit]
    def listSubscriptions(chatId: ChatId): Task[Set[RepositoryName]]
    def listRepositories: UIO[Map[RepositoryName, RepositoryStatus]]
  }

  trait InMemory extends Subscription {
    type SubscriberMap   = Map[RepositoryName, RepositoryStatus]
    type SubscriptionMap = Map[ChatId, Set[RepositoryName]]

    def subscribers: Ref[SubscriberMap]
    def subscriptions: Ref[SubscriptionMap]

    override val subscription: Subscription.Service = new Subscription.Service {
      override def subscribe(chatId: ChatId, repositoryName: RepositoryName): UIO[Unit] =
        updateSubscriptions(chatId)(_ + repositoryName) *>
          updateSubscribers(repositoryName)(_ + chatId) *>
          UIO.unit

      override def unsubscribe(chatId: ChatId, repositoryName: RepositoryName): UIO[Unit] =
        updateSubscriptions(chatId)(_.filterNot(_ == repositoryName)) *>
          updateSubscribers(repositoryName)(_.filterNot(_ == chatId)) *>
          UIO.unit

      override def listSubscriptions(chatId: ChatId): UIO[Set[RepositoryName]] =
        subscriptions.get.map(_.getOrElse(chatId, Set.empty))

      def listRepositories: UIO[SubscriberMap] =
        subscribers.get

      private def updateSubscriptions(chatId: ChatId)(f: Set[RepositoryName] => Set[RepositoryName]): UIO[SubscriptionMap] =
        subscriptions.update { current =>
          val subscriptions = current.getOrElse(chatId, Set.empty)
          current + (chatId -> f(subscriptions))
        }

      private def updateSubscribers(repositoryName: RepositoryName)(f: Set[ChatId] => Set[ChatId]): UIO[SubscriberMap] =
        subscribers.update { current =>
          val subscriptions = current.getOrElse(repositoryName, RepositoryStatus.empty)
          val updated       = subscriptions.copy(subscribers = f(subscriptions.subscribers))
          current + (repositoryName -> updated)
        }
    }
  }

  trait Doobie extends Subscription {
    override val subscription: Subscription.Service = new Subscription.Service {
      override def subscribe(chatId: ChatId, repositoryName: RepositoryName): UIO[Unit]   = ???
      override def unsubscribe(chatId: ChatId, repositoryName: RepositoryName): UIO[Unit] = ???
      override def listSubscriptions(chatId: ChatId): UIO[Set[RepositoryName]]            = ???
      override def listRepositories: UIO[Map[RepositoryName, RepositoryStatus]]           = ???
    }
  }

}
