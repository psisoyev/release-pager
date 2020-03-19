package io.pager.subscription.chat

import io.pager.client.telegram.ChatId
import io.pager.subscription.Repository.Name
import io.pager.subscription.chat.ChatStorage.{ Service, SubscriptionMap }
import zio.{ Ref, UIO }

private[chat] final case class InMemory(subscriptions: Ref[SubscriptionMap]) extends Service {
  type RepositoryUpdate = Set[Name] => Set[Name]

  override def subscribe(chatId: ChatId, name: Name): UIO[Unit] =
    updateSubscriptions(chatId)(_ + name).unit

  override def unsubscribe(chatId: ChatId, name: Name): UIO[Unit] =
    updateSubscriptions(chatId)(_ - name).unit

  private def updateSubscriptions(chatId: ChatId)(f: RepositoryUpdate): UIO[Unit] =
    subscriptions.update { current =>
      val subscriptions = current.getOrElse(chatId, Set.empty)
      current + (chatId -> f(subscriptions))
    }.unit

  override def listSubscriptions(chatId: ChatId): UIO[Set[Name]] =
    subscriptions
      .get
      .map(_.getOrElse(chatId, Set.empty))

  override def listSubscribers(name: Name): UIO[Set[ChatId]] =
    subscriptions
      .get
      .map(_.collect { case (chatId, repos) if repos.contains(name) => chatId }.toSet)
}
