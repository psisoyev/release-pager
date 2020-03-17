package io.pager.client.telegram.scenario

import canoe.api.{ TelegramClient => Client, _ }
import io.pager.subscription.SubscriptionLogic
import io.pager.subscription.SubscriptionLogic.SubscriptionLogic
import io.pager.validation.RepositoryValidator
import io.pager.validation.RepositoryValidator.RepositoryValidator
import zio._

object CanoeScenarios {
  type CanoeScenarios = Has[Service]

  trait Service {
    def start: Scenario[Task, Unit]
    def help: Scenario[Task, Unit]

    def add: Scenario[Task, Unit]
    def del: Scenario[Task, Unit]
    def list: Scenario[Task, Unit]
  }

  type LiveDeps = Has[Client[Task]] with RepositoryValidator with SubscriptionLogic
  def live: URLayer[LiveDeps, Has[Service]] =
    ZLayer.fromServices[Client[Task], RepositoryValidator.Service, SubscriptionLogic.Service, Service] {
      (client, repositoryValidator, subscriptionLogic) =>
        Live(repositoryValidator, subscriptionLogic, client)
    }
}
