package io.pager.client.telegram

import canoe.api.{ TelegramClient => Client }
import io.pager.client.telegram.scenario.CanoeScenarios
import io.pager.client.telegram.scenario.CanoeScenarios.CanoeScenarios
import io.pager.log.Logger
import io.pager.log.Logger.Logger
import zio._
import zio.macros.accessible

@accessible
object TelegramClient {
  type TelegramClient = Has[Service]

  trait Service {
    def start: Task[Unit]
    def broadcastMessage(receivers: Set[ChatId], message: String): Task[Unit]
  }

  type CanoeDeps = Has[Client[Task]] with Logger with CanoeScenarios
  def canoe: URLayer[CanoeDeps, Has[Service]] =
    ZLayer.fromServices[Client[Task], Logger.Service, CanoeScenarios.Service, Service] { (client, logger, scenarios) =>
      Canoe(logger, scenarios, client)
    }

  def empty: ULayer[Has[Service]] =
    ZLayer.succeed(new Service {
      override def start: Task[Unit]                                                     = ???
      override def broadcastMessage(receivers: Set[ChatId], message: String): Task[Unit] = ???
    })
}
