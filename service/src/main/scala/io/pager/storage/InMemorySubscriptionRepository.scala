package io.pager.storage

import io.pager.Subscription
import io.pager.Subscription.{ ChatId, RepositoryUrl }
import zio.{ Ref, UIO }

trait InMemorySubscriptionRepository extends SubscriptionRepository {
  def state: Ref[Map[ChatId, Set[RepositoryUrl]]]

  override val repository: SubscriptionRepository.Service = new SubscriptionRepository.Service {
    override def subscribe(subscription: Subscription): UIO[Unit] =
      state.update { current =>
        val subscriptions        = current.getOrElse(subscription.chatId, Set.empty)
        val updatedSubscriptions = subscriptions + subscription.url
        current + (subscription.chatId -> updatedSubscriptions)
      }.unit

    override def unsubscribe(subscription: Subscription): UIO[Unit] =
      state.update { current =>
        val subscriptions        = current.getOrElse(subscription.chatId, Set.empty)
        val updatedSubscriptions = subscriptions.filterNot(_ == subscription.url)
        current + (subscription.chatId -> updatedSubscriptions)
      }.unit

    override def list(chatId: ChatId): UIO[Set[RepositoryUrl]] =
      state.get.map(_.getOrElse(chatId, Set.empty))
  }
}
