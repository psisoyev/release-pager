package io.pager.subscription

import io.pager.client.telegram.ChatId
import io.pager.subscription.Repository.{ Name, Version }
import io.pager.subscription.SubscriptionLogic.SubscriptionLogic
import zio.test.mock._
import zio.{ Has, Task, URLayer, ZLayer }

object SubscriptionLogicMock extends Mock[SubscriptionLogic] {
  object subscribe         extends Effect[(ChatId, Name), Throwable, Unit]
  object unsubscribe       extends Effect[(ChatId, Name), Throwable, Unit]
  object listSubscriptions extends Effect[ChatId, Throwable, Set[Name]]
  object listRepositories  extends Effect[Unit, Throwable, Map[Name, Option[Version]]]
  object listSubscribers   extends Effect[Name, Throwable, Set[ChatId]]
  object updateVersions    extends Effect[Map[Name, Version], Throwable, Unit]

  val compose: URLayer[Has[Proxy], SubscriptionLogic] =
    ZLayer.fromService { proxy =>
      new SubscriptionLogic.Service {
        override def subscribe(chatId: ChatId, name: Name): Task[Unit] =
          proxy(SubscriptionLogicMock.subscribe, chatId, name)

        override def unsubscribe(chatId: ChatId, name: Name): Task[Unit] =
          proxy(SubscriptionLogicMock.unsubscribe, chatId, name)

        override def listSubscriptions(chatId: ChatId): Task[Set[Name]] =
          proxy(SubscriptionLogicMock.listSubscriptions, chatId)

        override def listRepositories: Task[Map[Name, Option[Version]]] =
          proxy(SubscriptionLogicMock.listRepositories)

        override def listSubscribers(name: Name): Task[Set[ChatId]] =
          proxy(SubscriptionLogicMock.listSubscribers, name)

        override def updateVersions(updatedVersions: Map[Name, Version]): Task[Unit] =
          proxy(SubscriptionLogicMock.updateVersions, updatedVersions)
      }
    }
}
