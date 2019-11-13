package io.pager

import io.pager.Subscription.{ ChatId, RepositoryName }
import zio.{ RIO, ZIO }

package object storage {

  def subscribe(subscription: Subscription): RIO[SubscriptionRepository, Unit] =
    ZIO.accessM[SubscriptionRepository](_.subscriptionRepository.subscribe(subscription))

  def unsubscribe(subscription: Subscription): RIO[SubscriptionRepository, Unit] =
    ZIO.accessM[SubscriptionRepository](_.subscriptionRepository.unsubscribe(subscription))

  def list(chatId: ChatId): RIO[SubscriptionRepository, Set[RepositoryName]] =
    ZIO.accessM[SubscriptionRepository](_.subscriptionRepository.listSubscriptions(chatId))
}
