package io.pager.subscription

//import zio.test.mock._

//@mockable[SubscriptionLogic]
//object SubscriptionLogicMock

// Using `mockable` we generate code below
//object SubscriptionLogicMock extends Mock[SubscriptionLogic] {
//  object Subscribe         extends Effect[(ChatId, Name), Throwable, Unit]
//  object Unsubscribe       extends Effect[(ChatId, Name), Throwable, Unit]
//  object ListSubscriptions extends Effect[ChatId, Throwable, Set[Name]]
//  object ListRepositories  extends Effect[Unit, Throwable, Map[Name, Option[Version]]]
//  object ListSubscribers   extends Effect[Name, Throwable, Set[ChatId]]
//  object UpdateVersions    extends Effect[Map[Name, Version], Throwable, Unit]
//
//  val compose: URLayer[Has[Proxy], SubscriptionLogic] =
//    ZLayer.fromService { proxy =>
//      new SubscriptionLogic {
//        override def subscribe(chatId: ChatId, name: Name): Task[Unit] =
//          proxy(SubscriptionLogicMock.Subscribe, chatId, name)
//
//        override def unsubscribe(chatId: ChatId, name: Name): Task[Unit] =
//          proxy(SubscriptionLogicMock.Unsubscribe, chatId, name)
//
//        override def listSubscriptions(chatId: ChatId): Task[Set[Name]] =
//          proxy(SubscriptionLogicMock.ListSubscriptions, chatId)
//
//        override def listRepositories: Task[Map[Name, Option[Version]]] =
//          proxy(SubscriptionLogicMock.ListRepositories)
//
//        override def listSubscribers(name: Name): Task[Set[ChatId]] =
//          proxy(SubscriptionLogicMock.ListSubscribers, name)
//
//        override def updateVersions(updatedVersions: Map[Name, Version]): Task[Unit] =
//          proxy(SubscriptionLogicMock.UpdateVersions, updatedVersions)
//      }
//    }
//}
