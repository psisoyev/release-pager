package io.pager.client.telegram

import canoe.api.models.ChatApi
import canoe.api.{ TelegramClient => Client, _ }
import canoe.models.PrivateChat
import canoe.models.outgoing.TextContent
import io.pager.logging.Logger
import zio._
import zio.interop.catz._
import zio.macros.annotation.{ accessible, mockable }

@mockable
@accessible(">")
trait TelegramClient {
  val telegramClient: TelegramClient.Service[Any]
}

object TelegramClient {
  trait Service[R] {
    def start: RIO[R, Unit]
    def broadcastMessage(subscribers: Set[ChatId], message: String): RIO[R, Unit]
  }

  trait Canoe extends TelegramClient {
    implicit def canoeClient: Client[Task]
    def logger: Logger.Service
    def scenarios: ScenarioLogic.Service[Scenario]

    override val telegramClient: Service[Any] = new Service[Any] {
      def broadcastMessage(subscribers: Set[ChatId], message: String): Task[Unit] =
        ZIO
          .traverse(subscribers) { chatId =>
            val api = new ChatApi(PrivateChat(chatId.value, None, None, None))
            api.send(TextContent(message))
          }
          .unit

      override def start: Task[Unit] =
        logger.info("Starting Telegram polling") *>
          Bot
            .polling[Task]
            .follow(
              scenarios.start,
              scenarios.help,
              scenarios.add,
              scenarios.del,
              scenarios.list
            )
            .compile
            .drain
    }
  }
}
