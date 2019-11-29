package io.pager.client.telegram

import canoe.api.models.ChatApi
import canoe.api.{ TelegramClient => Client, _ }
import canoe.models.PrivateChat
import canoe.models.outgoing.TextContent
import io.pager.logging.Logger
import zio._
import zio.interop.catz._

trait TelegramClient {
  val telegramClient: TelegramClient.Service
}

object TelegramClient {
  trait Service {
    def start: Task[Unit]
    def broadcastMessage(subscribers: Set[ChatId], message: String): Task[Unit]
  }

  trait Canoe extends TelegramClient {
    implicit def canoeClient: Client[Task]
    def logger: Logger.Service
    def scenarios: ScenarioLogic.Service[Scenario]

    override val telegramClient: Service = new Service {
      def broadcastMessage(subscribers: Set[ChatId], message: String): Task[Unit] =
        ZIO
          .traverse(subscribers) { chatId =>
            val api = new ChatApi(PrivateChat(chatId.value, None, None, None))
            api.send(TextContent(message))
          }
          .unit

      override def start: Task[Unit] =
        logger.info("starting telegram polling") *>
          Bot
            .polling[Task]
            .follow(scenarios.startBot, scenarios.help, scenarios.subscribe, scenarios.unsubscribe, scenarios.listRepositories)
            .compile
            .drain
    }
  }
}
