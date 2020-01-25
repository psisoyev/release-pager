package io.pager.subscription

import io.pager.client.telegram.ChatId
import io.pager.subscription.Repository.Name
import zio.{ RIO, Ref, Task, UIO }

trait ChatStorage {
  val chatStorage: ChatStorage.Service[Any]
}

object ChatStorage {
  type SubscriptionMap = Map[ChatId, Set[Name]]

  trait Service[R] {
    def subscribe(chatId: ChatId, name: Name): RIO[R, Unit]
    def unsubscribe(chatId: ChatId, name: Name): RIO[R, Unit]

    def listSubscriptions(chatId: ChatId): RIO[R, Set[Name]]
    def listSubscribers(name: Name): RIO[R, Set[ChatId]]
  }

  trait InMemory extends ChatStorage {
    def subscriptions: Ref[SubscriptionMap]
    type RepositoryUpdate = Set[Name] => Set[Name]

    val chatStorage: Service[Any] = new Service[Any] {
      override def subscribe(chatId: ChatId, name: Name): UIO[Unit] =
        updateSubscriptions(chatId)(_ + name).unit

      override def unsubscribe(chatId: ChatId, name: Name): UIO[Unit] =
        updateSubscriptions(chatId)(_ - name).unit

      private def updateSubscriptions(chatId: ChatId)(f: RepositoryUpdate): UIO[SubscriptionMap] =
        subscriptions.update { current =>
          val subscriptions = current.getOrElse(chatId, Set.empty)
          current + (chatId -> f(subscriptions))
        }

      override def listSubscriptions(chatId: ChatId): UIO[Set[Name]] =
        subscriptions
          .get
          .map(_.getOrElse(chatId, Set.empty))

      override def listSubscribers(name: Name): UIO[Set[ChatId]] =
        subscriptions
          .get
          .map(_.collect { case (chatId, repos) if repos.contains(name) => chatId }.toSet)
    }
  }

  trait Doobie extends ChatStorage {
    override val chatStorage: Service[Any] = new Service[Any] {
      def listSubscriptions(chatId: ChatId): Task[Set[Name]]  = ???
      def listSubscribers(name: Name): Task[Set[ChatId]]      = ???
      def subscribe(chatId: ChatId, name: Name): Task[Unit]   = ???
      def unsubscribe(chatId: ChatId, name: Name): Task[Unit] = ???
    }
  }

  object Test {
    def make(state: Ref[Map[ChatId, Set[Name]]]): ChatStorage.Service[Any] =
      new ChatStorage.InMemory { def subscriptions: Ref[SubscriptionMap] = state }.chatStorage
  }
}
