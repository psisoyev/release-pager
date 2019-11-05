package io.pager.storage

import io.pager.Subscription
import io.pager.Subscription.{ ChatId, RepositoryUrl }
import zio.UIO

trait SubscriptionRepository {
  val repository: SubscriptionRepository.Service
}

object SubscriptionRepository {
  trait Service {
    def subscribe(subscription: Subscription): UIO[Unit]
    def unsubscribe(subscription: Subscription): UIO[Unit]
    def listSubscriptions(chatId: ChatId): UIO[Set[RepositoryUrl]]
    def listRepositories: UIO[Set[RepositoryUrl]]
  }
}
