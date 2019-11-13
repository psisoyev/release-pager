package io.pager.storage

import io.pager.{ RepositoryStatus, Subscription }
import io.pager.Subscription.{ ChatId, RepositoryName }
import zio.{ Task, UIO }

trait SubscriptionRepository {
  val subscriptionRepository: SubscriptionRepository.Service
}

object SubscriptionRepository {
  trait Service {
    def subscribe(subscription: Subscription): UIO[Unit]
    def unsubscribe(subscription: Subscription): UIO[Unit]
    def listSubscriptions(chatId: ChatId): Task[Set[RepositoryName]]
    def listRepositories: UIO[Map[RepositoryName, RepositoryStatus]]
  }
}
