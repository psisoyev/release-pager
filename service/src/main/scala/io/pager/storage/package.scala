package io.pager

import io.pager.Subscription.{ ChatId, RepositoryUrl }
import zio.{ RIO, ZIO }

package object storage {

  def subscribe(subscription: Subscription): RIO[SubscriptionRepository, Unit] =
    ZIO.accessM[SubscriptionRepository](_.repository.subscribe(subscription))

  def unsubscribe(subscription: Subscription): RIO[SubscriptionRepository, Unit] =
    ZIO.accessM[SubscriptionRepository](_.repository.unsubscribe(subscription))

  def list(chatId: ChatId): RIO[SubscriptionRepository, Set[RepositoryUrl]] =
    ZIO.accessM[SubscriptionRepository](_.repository.listSubscriptions(chatId))
}
