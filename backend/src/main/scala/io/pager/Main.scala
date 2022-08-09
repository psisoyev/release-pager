package io.pager

import canoe.api.{ TelegramClient => CanoeClient }
import io.pager.client.github.GitHubClient
import io.pager.client.http.HttpClient
import io.pager.client.telegram.TelegramClient
import io.pager.client.telegram.scenario.CanoeScenarios
import io.pager.log.Logger
import io.pager.lookup.ReleaseChecker
import io.pager.subscription.SubscriptionLogic
import io.pager.subscription.chat.ChatStorage
import io.pager.subscription.repository.RepositoryVersionStorage
import io.pager.validation.RepositoryValidator
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.client.Client
import zio._
import zio.interop.catz._

object Main extends ZIOAppDefault {
  val http4sClient: RLayer[Scope, Client[Task]] = ZLayer {
    BlazeClientBuilder
      .apply[Task]
      .resource
      .toScopedZIO
  }

  val canoeClient: RLayer[Scope with Config, CanoeClient[Task]] = ZLayer {
    ZIO.service[Config].flatMap { config =>
      CanoeClient[Task](config.releasePager.botToken).toScopedZIO
    }
  }

  override def run: RIO[Scope, Unit] = {
    val program =
      for {
        flywayMigration <- ZIO.service[FlywayMigration]
        telegramClient  <- ZIO.service[TelegramClient]
        releaseChecker  <- ZIO.service[ReleaseChecker]
        _               <- flywayMigration.migrate
        _               <- telegramClient.start &> releaseChecker.scheduleRefresh.repeat(Schedule.spaced(1.minute))
      } yield ()

    program
      .provideSome[Scope](
        Config.live,
        http4sClient,
        HttpClient.http4s,
        canoeClient,
        Transactor.live,
        FlywayMigration.live,
        ReleaseChecker.live,
        CanoeScenarios.live,
        TelegramClient.canoe,
        RepositoryValidator.live,
        SubscriptionLogic.live,
        Logger.console,
        GitHubClient.live,
        ChatStorage.doobie,
        RepositoryVersionStorage.doobie
      )
  }
}
