package io.pager.subscription.chat

import doobie.util.transactor.Transactor
import io.pager.client.telegram.ChatId
import io.pager.subscription.Repository.Name
import zio._

object ChatStorage {
  type ChatStorage = Has[Service]

  type SubscriptionMap = Map[ChatId, Set[Name]]

  trait Service {
    def subscribe(chatId: ChatId, name: Name): Task[Unit]
    def unsubscribe(chatId: ChatId, name: Name): Task[Unit]

    def listSubscriptions(chatId: ChatId): Task[Set[Name]]
    def listSubscribers(name: Name): Task[Set[ChatId]]
  }

  val inMemory: ZLayer[Has[Ref[SubscriptionMap]], Nothing, Has[Service]] =
    ZLayer.fromService[Ref[SubscriptionMap], Service] { subscriptions =>
      InMemory(subscriptions)
    }

  val doobie: ZLayer[Has[Transactor[Task]], Nothing, Has[Service]] =
    ZLayer.fromService[Transactor[Task], Service] { xa: Transactor[Task] =>
      Doobie(xa)
    }
}
