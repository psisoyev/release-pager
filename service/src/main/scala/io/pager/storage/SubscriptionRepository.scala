package io.pager.storage

import io.pager.{ RepositoryStatus, Subscription }
import io.pager.Subscription.{ ChatId, RepositoryName }
import zio.UIO

trait SubscriptionRepository {
  val repository: SubscriptionRepository.Service
}

object SubscriptionRepository {
  trait Service {
    def subscribe(subscription: Subscription): UIO[Unit]
    def unsubscribe(subscription: Subscription): UIO[Unit]
    def listSubscriptions(chatId: ChatId): UIO[Set[RepositoryName]]
    def listRepositories: UIO[Map[RepositoryName, RepositoryStatus]]
  }
}
