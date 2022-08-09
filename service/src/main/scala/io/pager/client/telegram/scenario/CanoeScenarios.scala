package io.pager.client.telegram.scenario

import canoe.api.{ TelegramClient => Client, _ }
import io.pager.subscription.SubscriptionLogic
import io.pager.validation.RepositoryValidator
import zio._

trait CanoeScenarios {
  def start: Scenario[Task, Unit]

  def help: Scenario[Task, Unit]

  def add: Scenario[Task, Unit]

  def del: Scenario[Task, Unit]

  def list: Scenario[Task, Unit]
}

object CanoeScenarios {
  type LiveDeps = Client[Task] with RepositoryValidator with SubscriptionLogic
  val live: URLayer[LiveDeps, CanoeScenarios] = ZLayer {
    for {
      client    <- ZIO.service[Client[Task]]
      validator <- ZIO.service[RepositoryValidator]
      logic     <- ZIO.service[SubscriptionLogic]
    } yield Live(validator, logic, client)
  }
}
