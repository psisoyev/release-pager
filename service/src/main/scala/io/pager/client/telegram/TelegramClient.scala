package io.pager.client.telegram

import canoe.api.{ TelegramClient => Client, _ }
import io.pager.log.Logger
import io.pager.log.Logger.Logger
import io.pager.client.telegram.scenario.CanoeScenarios
import io.pager.client.telegram.scenario.CanoeScenarios.CanoeScenarios
import zio._
import zio.interop.catz._

object TelegramClient {
  type TelegramClient = Has[Service]

  trait Service {
    def start: Task[Unit]
    def broadcastMessage(subscribers: Set[ChatId], message: String): Task[Unit]
  }

  def canoe: ZLayer[Has[Client[Task]] with Logger with CanoeScenarios, Nothing, Has[Service]] =
    ZLayer.fromServices[Client[Task], Logger.Service, CanoeScenarios.Service, Service] { (client, logger, scenarios) =>
      new Canoe(logger, scenarios, client)
    }

  def empty: ULayer[Has[Service]] = ZLayer.succeed(new Service {
    override def start: Task[Unit]                                                       = ???
    override def broadcastMessage(subscribers: Set[ChatId], message: String): Task[Unit] = ???
  })

  def start: ZIO[TelegramClient, Throwable, Unit] = ZIO.accessM(_.get.start)
}
