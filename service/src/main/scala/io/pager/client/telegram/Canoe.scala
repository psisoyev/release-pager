package io.pager.client.telegram

import canoe.api.models.ChatApi
import canoe.api.{ TelegramClient => Client, _ }
import canoe.models.PrivateChat
import canoe.models.outgoing.TextContent
import io.pager.client.telegram.scenario.CanoeScenarios
import io.pager.log.Logger
import zio.interop.catz._
import zio.{ Task, ZIO }

private[telegram] final case class Canoe(
  logger: Logger.Service,
  scenarios: CanoeScenarios.Service,
  canoeClient: Client[Task]
) extends TelegramClient.Service {

  implicit val canoe: Client[Task] = canoeClient

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
