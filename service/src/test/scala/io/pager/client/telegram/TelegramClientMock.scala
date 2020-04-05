package io.pager.client.telegram

import io.pager.client.telegram.TelegramClient.TelegramClient
import zio.test.mock._
import zio.{ Has, Task, URLayer, ZLayer }

object TelegramClientMock extends Mock[TelegramClient] {
  object start            extends Effect[Unit, Throwable, Unit]
  object broadcastMessage extends Effect[(Set[ChatId], String), Throwable, Unit]

  val compose: URLayer[Has[Proxy], TelegramClient] =
    ZLayer.fromService { proxy =>
      new TelegramClient.Service {
        override def start: Task[Unit] =
          proxy(TelegramClientMock.start)

        override def broadcastMessage(subscribers: Set[ChatId], message: String): Task[Unit] =
          proxy(TelegramClientMock.broadcastMessage, subscribers, message)
      }
    }

}
