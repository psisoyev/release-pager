package io.pager.storage

import io.pager.Subscription
import io.pager.Subscription.{ ChatId, RepositoryUrl }
import zio.UIO

trait DoobieSubscriptionRepository extends SubscriptionRepository {
  override val repository: SubscriptionRepository.Service = new SubscriptionRepository.Service {
    override def subscribe(subscription: Subscription): UIO[Unit]           = ???
    override def unsubscribe(subscription: Subscription): UIO[Unit]         = ???
    override def listSubscriptions(chatId: ChatId): UIO[Set[RepositoryUrl]] = ???
    override def listRepositories(chatId: ChatId): UIO[Set[RepositoryUrl]]  = ???
  }
}
