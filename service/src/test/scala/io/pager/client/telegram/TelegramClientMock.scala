package io.pager.client.telegram

import io.pager.client.telegram.TelegramClient.TelegramClient
import zio.test.mock.Proxy
import zio.test.mock.Method
import zio.{ Has, Task, URLayer, ZLayer }

object TelegramClientMock {
  object start            extends Tag[Unit, Unit]
  object broadcastMessage extends Tag[(Set[ChatId], String), Unit]

  sealed class Tag[I, A] extends Method[TelegramClient, I, A] {
    override def envBuilder: URLayer[Has[Proxy], TelegramClient] = ZLayer.fromService { invoke =>
      new TelegramClient.Service {
        override def start: Task[Unit] =
          invoke(TelegramClientMock.start)
        override def broadcastMessage(subscribers: Set[ChatId], message: String): Task[Unit] =
          invoke(TelegramClientMock.broadcastMessage, subscribers, message)
      }
    }
  }

}
