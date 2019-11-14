package io.pager.subscription

import io.pager.{ RepositoryStatus, Subscription }
import io.pager.Subscription.{ ChatId, RepositoryName }
import zio.UIO

trait DoobieSubscriptionRepository extends SubscriptionRepository {
  override val subscriptionRepository: SubscriptionRepository.Service = new SubscriptionRepository.Service {
    override def subscribe(subscription: Subscription): UIO[Unit]             = ???
    override def unsubscribe(subscription: Subscription): UIO[Unit]           = ???
    override def listSubscriptions(chatId: ChatId): UIO[Set[RepositoryName]]  = ???
    override def listRepositories: UIO[Map[RepositoryName, RepositoryStatus]] = ???
  }
}
