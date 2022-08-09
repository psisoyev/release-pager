package io.pager.subscription

import io.pager.client.telegram.ChatId
import io.pager.subscription.Repository.Name
import io.pager.subscription.Subscription.SubscriptionId

import java.util.UUID

final case class Subscription(id: SubscriptionId, chatId: ChatId, name: Name)

object Subscription {
  final case class SubscriptionId(value: String)

  def make(chatId: ChatId, name: Name): Subscription =
    Subscription(
      SubscriptionId(UUID.randomUUID().toString),
      chatId,
      name
    )
}
