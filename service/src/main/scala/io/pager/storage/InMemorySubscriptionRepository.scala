package io.pager.storage

import io.pager.{RepositoryStatus, Subscription}
import io.pager.Subscription.{ChatId, RepositoryUrl}
import zio.{Ref, UIO}

trait InMemorySubscriptionRepository extends SubscriptionRepository {
  type SubscriberMap = Map[RepositoryUrl, RepositoryStatus]
  type SubscriptionMap = Map[ChatId, Set[RepositoryUrl]]

  def subscribers: Ref[SubscriberMap]
  def subscriptions: Ref[SubscriptionMap]

  override val repository: SubscriptionRepository.Service = new SubscriptionRepository.Service {
    override def subscribe(subscription: Subscription): UIO[Unit] =
      updateSubscriptions(subscription)(_ + subscription.url) *>
        updateSubscribers(subscription)(_ + subscription.chatId) *>
        UIO.unit

    override def unsubscribe(subscription: Subscription): UIO[Unit] =
      updateSubscriptions(subscription)(_.filterNot(_ == subscription.url)) *>
        updateSubscribers(subscription)(_.filterNot(_ == subscription.chatId)) *>
        UIO.unit

    override def listSubscriptions(chatId: ChatId): UIO[Set[RepositoryUrl]] =
      subscriptions.get.map(_.getOrElse(chatId, Set.empty))

    def listRepositories(chatId: ChatId): UIO[Set[RepositoryUrl]] =
      subscribers.get.map(_.keySet)

    private def updateSubscriptions(subscription: Subscription)(f: Set[RepositoryUrl] => Set[RepositoryUrl]): UIO[SubscriptionMap] =
      subscriptions.update { current =>
        val subscriptions = current.getOrElse(subscription.chatId, Set.empty)
        current + (subscription.chatId -> f(subscriptions))
      }

    private def updateSubscribers(subscription: Subscription)(f: Set[ChatId] => Set[ChatId]): UIO[SubscriberMap] =
      subscribers.update { current =>
        val subscriptions = current.getOrElse(subscription.url, RepositoryStatus.empty)
        val updated = subscriptions.copy(subscribers = f(subscriptions.subscribers))
        current + (subscription.url -> updated)
      }
  }
}
