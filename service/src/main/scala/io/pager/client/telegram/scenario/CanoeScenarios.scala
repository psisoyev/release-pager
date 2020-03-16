package io.pager.client.telegram.scenario

import canoe.api.{ TelegramClient => Client, _ }
import canoe.models.Chat
import canoe.models.messages.TextMessage
import canoe.syntax._
import io.pager.PagerError
import io.pager.subscription.Repository.Name
import io.pager.subscription.SubscriptionLogic
import io.pager.subscription.SubscriptionLogic.SubscriptionLogic
import io.pager.validation.RepositoryValidator
import io.pager.validation.RepositoryValidator.RepositoryValidator
import io.pager.client.telegram.scenario.Live
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

  def live: ZLayer[Has[Client[Task]] with RepositoryValidator with SubscriptionLogic, Nothing, Has[Service]] =
    ZLayer.fromServices[Client[Task], RepositoryValidator.Service, SubscriptionLogic.Service, Service] {
      (client, repositoryValidator, subscriptionLogic) =>
        Live(repositoryValidator, subscriptionLogic, client)
    }
}
