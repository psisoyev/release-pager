package io.pager

import canoe.api.{ TelegramClient => CanoeClient }
import cats.effect.{ Blocker, Resource }
import doobie.hikari.HikariTransactor
import doobie.util.transactor.Transactor
import io.pager.Config.DBConfig
import io.pager.PagerError.{ ConfigurationError, MissingBotToken }
import io.pager.client.github.GitHubClient
import io.pager.client.http.HttpClient
import io.pager.client.telegram.TelegramClient
import io.pager.client.telegram.scenario.CanoeScenarios
import io.pager.log.Logger
import io.pager.lookup.ReleaseChecker
import io.pager.subscription.chat.ChatStorage
import io.pager.subscription.SubscriptionLogic
import io.pager.subscription.repository.RepositoryVersionStorage
import io.pager.validation.RepositoryValidator
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import pureconfig.ConfigSource
import zio._
import zio.blocking.Blocking
import zio.console.putStrLn
import zio.duration._
import zio.interop.catz._
import zio.system._
import pureconfig.generic.auto._

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits

object Main extends zio.App {

  override def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] = {
    val program = for {
      token <- telegramBotToken orElse UIO.succeed("972654063:AAEOiS2tpJkrPNsIMLI7glUUvNCjxpJ_2T8")

      config <- readConfig
      _      <- FlywayMigration.migrate(config.releasePager.dbConfig)

      http4sClient <- makeHttpClient
      canoeClient  <- makeCanoeClient(token)
      transactor   <- makeTransactor(config.releasePager.dbConfig)

      _ <- makeProgram(http4sClient, canoeClient, transactor)
    } yield ()

    program.foldM(
      err => putStrLn(s"Execution failed with: ${err.getMessage}") *> ZIO.succeed(ExitCode.failure),
      _ => ZIO.succeed(ExitCode.success)
    )
  }

  private def telegramBotToken: RIO[System, String] =
    for {
      token <- system.env("BOT_TOKEN")
      token <- ZIO.fromOption(token).mapError(_ => MissingBotToken)
    } yield token

  private def makeHttpClient: UIO[TaskManaged[Client[Task]]] =
    ZIO
      .runtime[Any]
      .map { implicit rts =>
        BlazeClientBuilder
          .apply[Task](Implicits.global)
          .resource
          .toManaged
      }

  private def makeCanoeClient(token: String): UIO[TaskManaged[CanoeClient[Task]]] =
    ZIO
      .runtime[Any]
      .map { implicit rts =>
        CanoeClient
          .global[Task](token)
          .toManaged
      }

  private def makeTransactor(config: DBConfig): RIO[Blocking, RManaged[Blocking, HikariTransactor[Task]]] = {
    def transactor(connectEC: ExecutionContext, transactEC: ExecutionContext): Resource[Task, HikariTransactor[Task]] =
      HikariTransactor.newHikariTransactor[Task](
        config.driver,
        config.url,
        config.user,
        config.password,
        connectEC,
        Blocker.liftExecutionContext(transactEC)
      )

    ZIO
      .runtime[Blocking]
      .map { implicit rt =>
        for {
          transactEC <- ZIO.access[Blocking](_.get.blockingExecutor.asEC).toManaged_
          transactor <- transactor(rt.platform.executor.asEC, transactEC).toManaged
        } yield transactor
      }
  }

  private def readConfig: IO[ConfigurationError, Config] =
    ZIO
      .fromEither(ConfigSource.default.load[Config])
      .mapError(failures => ConfigurationError(failures.prettyPrint()))

  // format: off
  private def makeProgram(
    http4sClient: TaskManaged[Client[Task]],
    canoeClient: TaskManaged[CanoeClient[Task]],
    transactor: RManaged[Blocking, Transactor[Task]]
  ): RIO[ZEnv, Int] = {
    val loggerLayer = Logger.console
    val transactorLayer = transactor.toLayer.orDie

    val chatStorageLayer = transactorLayer >>> ChatStorage.doobie
    val repositoryVersionStorageLayer = transactorLayer >>> RepositoryVersionStorage.doobie
    val storageLayer = chatStorageLayer ++ repositoryVersionStorageLayer
    val subscriptionLogicLayer = (loggerLayer ++ storageLayer) >>> SubscriptionLogic.live

    val http4sClientLayer = http4sClient.toLayer.orDie
    val httpClientLayer = http4sClientLayer >>> HttpClient.http4s
    val gitHubClientLayer = (loggerLayer ++ httpClientLayer) >>> GitHubClient.live
    val repositoryValidatorLayer = (loggerLayer ++ gitHubClientLayer) >>> RepositoryValidator.live

    val canoeClientLayer = canoeClient.toLayer.orDie

    val canoeScenarioLayer = (canoeClientLayer ++ repositoryValidatorLayer ++ subscriptionLogicLayer) >>> CanoeScenarios.live // TODO if you swap - it doesnt compile
    val telegramClientLayer = (loggerLayer ++ canoeScenarioLayer ++ canoeClientLayer) >>> TelegramClient.canoe
    val releaseCheckerLayer = (loggerLayer ++ gitHubClientLayer ++ telegramClientLayer ++ subscriptionLogicLayer) >>> ReleaseChecker.live

    val startTelegramClient = TelegramClient.start
    val scheduleRefresh = ReleaseChecker.scheduleRefresh.repeat(Schedule.spaced(1.minute))

    val programLayer = releaseCheckerLayer ++ telegramClientLayer
    val program = startTelegramClient.fork *> scheduleRefresh

    program.provideSomeLayer[ZEnv](programLayer)
  }
}
