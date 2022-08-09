package io.pager.subscription.chat

import doobie.util.transactor.Transactor
import io.pager.client.telegram.ChatId
import io.pager.subscription.Repository.Name
import zio._

trait ChatStorage {
  def subscribe(chatId: ChatId, name: Name): Task[Unit]
  def unsubscribe(chatId: ChatId, name: Name): Task[Unit]

  def listSubscriptions(chatId: ChatId): Task[Set[Name]]
  def listSubscribers(name: Name): Task[Set[ChatId]]
}

object ChatStorage {
  type SubscriptionMap = Map[ChatId, Set[Name]]

  val inMemory: ZLayer[Ref[SubscriptionMap], Nothing, ChatStorage] = ZLayer {
    ZIO.service[Ref[SubscriptionMap]].map(InMemory)
  }

  val doobie: ZLayer[Transactor[Task], Nothing, ChatStorage] = ZLayer {
    ZIO.service[Transactor[Task]].map(Doobie)
  }
}
