package io.pager.subscription

import doobie.implicits._
import doobie.util.transactor.Transactor
import doobie.{ Query0, Update0 }
import io.pager.client.telegram.ChatId
import io.pager.subscription.ChatStorage.Doobie.SQL
import io.pager.subscription.Repository.Name
import zio.interop.catz._
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
    def xa: Transactor[Task]

    override val chatStorage: Service[Any] = new Service[Any] {
      def subscribe(chatId: ChatId, name: Name): Task[Unit] =
        SQL
          .create(chatId, name)
          .withUniqueGeneratedKeys[Long]("ID")
          .transact(xa)
          .unit
          .orDie

      def unsubscribe(chatId: ChatId, name: Name): Task[Unit] =
        SQL
          .delete(chatId, name)
          .run
          .transact(xa)
          .unit
          .orDie

      def listSubscriptions(chatId: ChatId): Task[Set[Name]] =
        SQL
          .getByChat(chatId)
          .to[Set]
          .map(_.map(_.name))
          .transact(xa)
          .orDie

      def listSubscribers(name: Name): Task[Set[ChatId]] =
        SQL
          .getByName(name)
          .to[Set]
          .map(_.map(_.chatId))
          .transact(xa)
          .orDie
    }
  }

  object Doobie {
    object SQL {
      def create(chatId: ChatId, name: Name): Update0 =
        sql"""
      INSERT INTO SUBSCRIPTION (CHAT_ID, REPOSITORY_NAME)
      VALUES (${chatId.value}, ${name.value})
      """.update

      def delete(chatId: ChatId, name: Name): Update0 =
        sql"""
      DELETE from SUBSCRIPTION
      WHERE CHAT_ID = ${chatId.value} AND REPOSITORY_NAME = ${name.value}
      """.update

      def getByChat(chatId: ChatId): Query0[Subscription] = sql"""
      SELECT * FROM SUBSCRIPTION WHERE CHAT_ID = ${chatId.value}
      """.query[Subscription]

      def getByName(name: Name): Query0[Subscription] = sql"""
      SELECT * FROM SUBSCRIPTION WHERE REPOSITORY_NAME = ${name.value}
      """.query[Subscription]

      def update(subscription: Subscription): Update0 =
        sql"""
      UPDATE SUBSCRIPTION SET
      CHAT_ID = ${subscription.chatId.value},
      REPOSITORY_NAME = ${subscription.name.value}
      WHERE ID = ${subscription.id.value}
      """.update
    }
  }

  object Test {
    def make(state: Ref[Map[ChatId, Set[Name]]]): ChatStorage.Service[Any] =
      new ChatStorage.InMemory { def subscriptions: Ref[SubscriptionMap] = state }.chatStorage
  }
}
