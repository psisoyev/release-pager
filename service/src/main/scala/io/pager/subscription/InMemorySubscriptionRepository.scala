package io.pager.subscription

import io.pager.{ RepositoryStatus, Subscription }
import io.pager.Subscription.{ ChatId, RepositoryName }
import zio.{ Ref, UIO }

trait InMemorySubscriptionRepository extends SubscriptionRepository {
  type SubscriberMap   = Map[RepositoryName, RepositoryStatus]
  type SubscriptionMap = Map[ChatId, Set[RepositoryName]]

  def subscribers: Ref[SubscriberMap]
  def subscriptions: Ref[SubscriptionMap]

  override val subscriptionRepository: SubscriptionRepository.Service = new SubscriptionRepository.Service {
    override def subscribe(subscription: Subscription): UIO[Unit] =
      updateSubscriptions(subscription)(_ + subscription.name) *>
        updateSubscribers(subscription)(_ + subscription.chatId) *>
        UIO.unit

    override def unsubscribe(subscription: Subscription): UIO[Unit] =
      updateSubscriptions(subscription)(_.filterNot(_ == subscription.name)) *>
        updateSubscribers(subscription)(_.filterNot(_ == subscription.chatId)) *>
        UIO.unit

    override def listSubscriptions(chatId: ChatId): UIO[Set[RepositoryName]] =
      subscriptions.get.map(_.getOrElse(chatId, Set.empty))

    def listRepositories: UIO[SubscriberMap] =
      subscribers.get

    private def updateSubscriptions(subscription: Subscription)(f: Set[RepositoryName] => Set[RepositoryName]): UIO[SubscriptionMap] =
      subscriptions.update { current =>
        val subscriptions = current.getOrElse(subscription.chatId, Set.empty)
        current + (subscription.chatId -> f(subscriptions))
      }

    private def updateSubscribers(subscription: Subscription)(f: Set[ChatId] => Set[ChatId]): UIO[SubscriberMap] =
      subscribers.update { current =>
        val subscriptions = current.getOrElse(subscription.name, RepositoryStatus.empty)
        val updated       = subscriptions.copy(subscribers = f(subscriptions.subscribers))
        current + (subscription.name -> updated)
      }
  }
}
