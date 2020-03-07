package io.pager.client.telegram

import canoe.api.models.ChatApi
import canoe.api.{ TelegramClient => Client, _ }
import canoe.models.PrivateChat
import canoe.models.outgoing.TextContent
import io.pager.logging.Logger
import zio._
import zio.interop.catz._

object TelegramClient {
  type TelegramClient = Has[Service]

  trait Service {
    def start: Task[Unit]
    def broadcastMessage(subscribers: Set[ChatId], message: String): Task[Unit]
  }

  class Canoe(logger: Logger.Service, scenarios: ScenarioLogic.Service[Scenario])(implicit canoeClient: Client[Task]) extends Service {
    def broadcastMessage(subscribers: Set[ChatId], message: String): Task[Unit] =
      ZIO
        .foreach(subscribers) { chatId =>
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

  def canoe: ZLayer[Has[Logger.Service] with Has[ScenarioLogic.Service[Scenario]], Nothing, Has[Canoe]] =
    ZLayer.fromServices[Logger.Service, ScenarioLogic.Service[Scenario], Canoe] {
      (logger: Logger.Service, scenarios: ScenarioLogic.Service[Scenario]) =>
        implicit val client: Client[Task] = ???
        new Canoe(logger, scenarios)
    }
}
