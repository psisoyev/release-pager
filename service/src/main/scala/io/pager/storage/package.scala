package io.pager

import io.pager.Subscription.{ ChatId, RepositoryUrl }
import zio.ZIO

package object storage {

  def subscribe(subscription: Subscription): ZIO[SubscriptionRepository, Throwable, Unit] =
    ZIO.accessM[SubscriptionRepository](_.repository.subscribe(subscription))

  def unsubscribe(subscription: Subscription): ZIO[SubscriptionRepository, Throwable, Unit] =
    ZIO.accessM[SubscriptionRepository](_.repository.unsubscribe(subscription))

  def list(chatId: ChatId): ZIO[AppEnv, Throwable, Set[RepositoryUrl]] =
    ZIO.accessM[SubscriptionRepository](_.repository.list(chatId))
}
