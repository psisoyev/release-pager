package io.pager.storage

import io.pager.{ PagerError, Subscription }
import zio.ZIO

trait InMemorySubscriptionRepository extends SubscriptionRepository {
  override val repository: SubscriptionRepository.Service = new SubscriptionRepository.Service {
    override def subscribe(subscription: Subscription): ZIO[Any, PagerError, Unit] =
      ???
    override def unsubscribe(subscription: Subscription): ZIO[Any, PagerError, Unit] = ???
  }
}
