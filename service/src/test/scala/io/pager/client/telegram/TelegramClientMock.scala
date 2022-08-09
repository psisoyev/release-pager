package io.pager.client.telegram

//import zio.test.mock._
//
//@mockable[TelegramClient]
//object TelegramClientMock

//object TelegramClientMock extends Mock[TelegramClient] {
//  object Start            extends Effect[Unit, Throwable, Unit]
//  object BroadcastMessage extends Effect[(Set[ChatId], String), Throwable, Unit]
//
//  val compose: URLayer[Has[Proxy], TelegramClient] =
//    ZLayer.fromService { proxy =>
//      new TelegramClient {
//        override def start: Task[Unit] =
//          proxy(TelegramClientMock.Start)
//
//        override def broadcastMessage(subscribers: Set[ChatId], message: String): Task[Unit] =
//          proxy(TelegramClientMock.BroadcastMessage, subscribers, message)
//      }
//    }
//}
