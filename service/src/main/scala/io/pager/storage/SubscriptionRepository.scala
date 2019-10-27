package io.pager.storage

import io.pager.{ PagerError, Subscription }
import zio.ZIO

trait SubscriptionRepository {
  val repository: SubscriptionRepository.Service
}

object SubscriptionRepository {
  trait Service {
    def subscribe(subscription: Subscription): ZIO[Any, PagerError, Unit]
    def unsubscribe(subscription: Subscription): ZIO[Any, PagerError, Unit]
  }
}
