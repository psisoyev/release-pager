package io.pager.subscription

import doobie.util.transactor.Transactor
import doobie.implicits._
import doobie.util.query.Query0
import doobie.util.update.Update0
import io.pager.client.telegram.ChatId
import io.pager.subscription.Repository.Name
import zio.interop.catz._
import zio._

object ChatStorage {
  type ChatStorage = Has[Service]

  type SubscriptionMap          = Map[ChatId, Set[Name]]
  private type RepositoryUpdate = Set[Name] => Set[Name]

  trait Service {
    def subscribe(chatId: ChatId, name: Name): Task[Unit]
    def unsubscribe(chatId: ChatId, name: Name): Task[Unit]

    def listSubscriptions(chatId: ChatId): Task[Set[Name]]
    def listSubscribers(name: Name): Task[Set[ChatId]]
  }

  private final case class InMemory(subscriptions: Ref[SubscriptionMap]) extends Service {
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

  val inMemory: ZLayer[Ref[SubscriptionMap], Nothing, Has[Service]] =
    ZLayer.fromFunction { subscriptions: Ref[SubscriptionMap] =>
      InMemory(subscriptions)
    }

  private final case class Doobie(xa: Transactor[Task]) extends Service {
    override def subscribe(chatId: ChatId, name: Name): Task[Unit] =
      SQL
        .create(chatId, name)
        .withUniqueGeneratedKeys[Long]("ID")
        .transact(xa)
        .unit
        .orDie

    override def unsubscribe(chatId: ChatId, name: Name): Task[Unit] =
      SQL
        .delete(chatId, name)
        .run
        .transact(xa)
        .unit
        .orDie

    override def listSubscriptions(chatId: ChatId): Task[Set[Name]] =
      SQL
        .getByChat(chatId)
        .to[Set]
        .map(_.map(_.name))
        .transact(xa)
        .orDie

    override def listSubscribers(name: Name): Task[Set[ChatId]] =
      SQL
        .getByName(name)
        .to[Set]
        .map(_.map(_.chatId))
        .transact(xa)
        .orDie
  }

  val doobie: ZLayer[Has[Transactor[Task]], Nothing, Has[Service]] =
    ZLayer.fromService[Transactor[Task], Service] { xa: Transactor[Task] =>
      Doobie(xa)
    }

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
