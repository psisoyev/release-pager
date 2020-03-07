package io.pager

import canoe.api.{ TelegramClient => CanoeClient }
import cats.effect.{ Blocker, Resource }
import doobie.hikari.HikariTransactor
import doobie.util.transactor.Transactor
import io.pager.Config.DBConfig
import io.pager.PagerError.{ ConfigurationError, MissingBotToken }
import io.pager.client.github.GitHubClient
import io.pager.client.http.HttpClient
import io.pager.client.telegram.{ ScenarioLogic, TelegramClient }
import io.pager.logging._
import io.pager.lookup.ReleaseChecker
import io.pager.subscription.{ ChatStorage, RepositoryVersionStorage, SubscriptionLogic }
import io.pager.validation.RepositoryValidator
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import pureconfig.ConfigSource
import zio._
import zio.blocking.Blocking
import zio.clock.Clock
import zio.console.{ putStrLn, Console }
import zio.duration._
import zio.interop.catz._
import zio.system._
import pureconfig.generic.auto._

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits

object Main extends zio.App {

  override def run(args: List[String]): ZIO[ZEnv, Nothing, Int] = {
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
      err => putStrLn(s"Execution failed with: ${err.getMessage}") *> ZIO.succeed(1),
      _ => ZIO.succeed(0)
    )
  }

  private def telegramBotToken: RIO[System, String] =
    for {
      token <- system.env("BOT_TOKEN")
      token <- ZIO.fromOption(token).asError(MissingBotToken)
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
      HikariTransactor
        .newHikariTransactor[Task](
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
          transactEC <- executionContext(rt)
          transactor <- transactor(rt.platform.executor.asEC, transactEC).toManaged
        } yield transactor
      }
  }

  private def executionContext(rt: Runtime[Blocking]): UManaged[ExecutionContext] =
    rt.environment
      .blocking
      .blockingExecutor
      .map(_.asEC)
      .toManaged_

  private def readConfig: IO[ConfigurationError, Config] =
    ZIO
      .fromEither(ConfigSource.default.load[Config])
      .mapError(failures => ConfigurationError(failures.prettyPrint()))

  // format: off
  private def makeProgram(
    http4sClient: TaskManaged[Client[Task]],
    canoeClient: TaskManaged[CanoeClient[Task]],
    transactor: RManaged[Blocking, HikariTransactor[Task]]
  ): RIO[ZEnv, Int] = {
    val startTelegramClient = TelegramClient.>.start
    val scheduleRefresh = ReleaseChecker.>.scheduleRefresh

    val program =
      startTelegramClient.fork *>
        scheduleRefresh.repeat(Schedule.spaced(1.minute))

    val xxx = transactor.flatMap(ChatStorage.doobie.build.provide)

    canoeClient.use { globalCanoeClient =>
      http4sClient.use { http4sClient =>
        transactor.use { transactor =>
          val xx: ZIO[ChatStorage.Service, Throwable, Int] = program.provideSome[ChatStorage.Service] { chatStorageImpl =>
            new TelegramClient.Canoe
              with ScenarioLogic.CanoeScenarios
              with Logger.Console
              with SubscriptionLogic.Live
              with RepositoryVersionStorage.Doobie
              with RepositoryValidator.GitHub
              with GitHubClient.Live
              with HttpClient.Http4s
              with ReleaseChecker.Live {
              override def xa: Transactor[Task] = transactor
              override def client: Client[Task] = http4sClient
              override implicit def canoeClient: CanoeClient[Task] = globalCanoeClient
              override def chatStorage: ChatStorage.Service = chatStorageImpl
            }
          }

          xx.provideLayer(xxx)
        }
      }
    }
  }
}
