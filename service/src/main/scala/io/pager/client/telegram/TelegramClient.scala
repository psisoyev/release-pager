package io.pager.client.telegram

import canoe.api.{ TelegramClient => Client }
import io.pager.client.telegram.scenario.CanoeScenarios
import io.pager.log.Logger
import zio._

trait TelegramClient {
  def start: Task[Unit]

  def broadcastMessage(receivers: Set[ChatId], message: String): Task[Unit]
}

object TelegramClient {
  type CanoeDeps = Client[Task] with Logger with CanoeScenarios
  def canoe: URLayer[CanoeDeps, TelegramClient] = ZLayer {
    for {
      client    <- ZIO.service[Client[Task]]
      logger    <- ZIO.service[Logger]
      scenarios <- ZIO.service[CanoeScenarios]
    } yield Canoe(logger, scenarios, client)
  }

  def empty: ULayer[TelegramClient] =
    ZLayer.succeed(new TelegramClient {
      override def start: Task[Unit]                                                     = ???
      override def broadcastMessage(receivers: Set[ChatId], message: String): Task[Unit] = ???
    })
}
