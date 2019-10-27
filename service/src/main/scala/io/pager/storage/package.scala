package io.pager

import zio.ZIO

package object storage {
  def subscribe(subscription: Subscription): ZIO[SubscriptionRepository, PagerError, Unit] =
    ZIO.accessM[SubscriptionRepository](_.repository.subscribe(subscription))
  def unsubscribe(subscription: Subscription): ZIO[SubscriptionRepository, PagerError, Unit] =
    ZIO.accessM[SubscriptionRepository](_.repository.unsubscribe(subscription))
}
