package io.pager.subscription.chat

import doobie.implicits._
import doobie.util.query.Query0
import doobie.util.transactor.Transactor
import doobie.util.update.Update0
import io.pager.client.telegram.ChatId
import io.pager.subscription.Repository.Name
import io.pager.subscription.Subscription
import zio.Task
import zio.interop.catz._

private[chat] final case class Doobie(xa: Transactor[Task]) extends ChatStorage {
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

private object SQL {
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
