package io.pager.subscription

import io.pager.client.telegram.ChatId
import io.pager.subscription.Repository.{ Name, Version }
import io.pager.subscription.SubscriptionLogic.SubscriptionLogic
import zio.test.mock.Proxy
import zio.test.mock.Method
import zio.{ Has, Task, URLayer, ZLayer }

object SubscriptionLogicMock {
  object subscribe         extends Tag[(ChatId, Name), Unit]
  object unsubscribe       extends Tag[(ChatId, Name), Unit]
  object listSubscriptions extends Tag[ChatId, Set[Name]]
  object listRepositories  extends Tag[Unit, Map[Name, Option[Version]]]
  object listSubscribers   extends Tag[Name, Set[ChatId]]
  object updateVersions    extends Tag[Map[Name, Version], Unit]

  sealed class Tag[I, A] extends Method[SubscriptionLogic, I, A] {
    override def envBuilder: URLayer[Has[Proxy], SubscriptionLogic] = ZLayer.fromService { invoke =>
      new SubscriptionLogic.Service {
        override def subscribe(chatId: ChatId, name: Name): Task[Unit]               = invoke(SubscriptionLogicMock.subscribe, chatId, name)
        override def unsubscribe(chatId: ChatId, name: Name): Task[Unit]             = invoke(SubscriptionLogicMock.unsubscribe, chatId, name)
        override def listSubscriptions(chatId: ChatId): Task[Set[Name]]              = invoke(SubscriptionLogicMock.listSubscriptions, chatId)
        override def listRepositories: Task[Map[Name, Option[Version]]]              = invoke(SubscriptionLogicMock.listRepositories)
        override def listSubscribers(name: Name): Task[Set[ChatId]]                  = invoke(SubscriptionLogicMock.listSubscribers, name)
        override def updateVersions(updatedVersions: Map[Name, Version]): Task[Unit] = invoke(SubscriptionLogicMock.updateVersions, updatedVersions)
      }
    }
  }
}
