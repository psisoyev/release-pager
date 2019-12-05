package io.pager

import java.util.concurrent.TimeUnit

import canoe.api.{ TelegramClient => CanoeClient }
import cats.effect.Resource
import io.pager.PagerError.MissingBotToken
import io.pager.client.github.GitHubClient
import io.pager.client.http.HttpClient
import io.pager.client.telegram.{ ChatId, ScenarioLogic, TelegramClient }
import io.pager.logging._
import io.pager.lookup.ReleaseChecker
import io.pager.subscription.ChatStorage.SubscriptionMap
import io.pager.subscription.Repository.{ Name, Version }
import io.pager.subscription.RepositoryVersionStorage.SubscriberMap
import io.pager.subscription.{ ChatStorage, RepositoryVersionStorage, SubscriptionLogic }
import io.pager.validation.RepositoryValidator
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import zio._
import zio.clock.Clock
import zio.console.{ putStrLn, Console }
import zio.duration.Duration
import zio.interop.catz._
import zio.system._

import scala.concurrent.ExecutionContext.Implicits

object Main extends zio.App {
  type ReleaseCheckerEnv = ReleaseChecker with Clock

  override def run(args: List[String]): ZIO[ZEnv, Nothing, Int] = {
    val program = for {
      token <- telegramBotToken

      subscriberMap   <- Ref.make(Map.empty[Name, Option[Version]])
      subscriptionMap <- Ref.make(Map.empty[ChatId, Set[Name]])

      http4sClient <- buildHttpClient
      canoeClient  <- buildTelegramClient(token)

      _ <- buildProgram(subscriberMap, subscriptionMap, http4sClient, canoeClient)
    } yield ()

    program.foldM(
      err => putStrLn(s"Execution failed with: $err") *> ZIO.succeed(1),
      _ => ZIO.succeed(0)
    )
  }

  private def telegramBotToken: RIO[System, String] =
    for {
      token <- system.env("BOT_TOKEN")
      token <- ZIO.fromOption(token).mapError(_ => MissingBotToken)
    } yield token

  private def buildHttpClient: RIO[ZEnv, Resource[Task, Client[Task]]] =
    ZIO
      .runtime[ZEnv]
      .map { implicit rts =>
        BlazeClientBuilder
          .apply[Task](Implicits.global)
          .resource
      }

  private def buildTelegramClient(token: String): RIO[ZEnv, Resource[Task, CanoeClient[Task]]] =
    ZIO
      .runtime[ZEnv]
      .map { implicit rts =>
        CanoeClient.global[Task](token)
      }

  private def buildProgram(
    subscriberMap: Ref[Map[Name, Option[Version]]],
    subscriptionMap: Ref[Map[ChatId, Set[Name]]],
    http4sClient: Resource[Task, Client[Task]],
    canoeClient: Resource[Task, CanoeClient[Task]]
  ): Task[Int] = {
    val startTelegramClient = ZIO.accessM[TelegramClient](_.telegramClient.start).fork
    val scheduleReleaseChecker =
      ZIO
        .accessM[ReleaseCheckerEnv](_.releaseChecker.scheduleRefresh)
        .repeat(Schedule.fixed(Duration(1, TimeUnit.MINUTES)))
    val program = startTelegramClient *> scheduleReleaseChecker

    canoeClient.use { globalCanoeClient =>
      http4sClient.use { http4sClient =>
        program.provide {
          new TelegramClient.Canoe
            with ScenarioLogic.CanoeScenarios
            with Clock.Live
            with Logger.Console
            with Console.Live
            with SubscriptionLogic.Live
            with ChatStorage.InMemory
            with RepositoryVersionStorage.InMemory
            with RepositoryValidator.GitHub
            with GitHubClient.Live
            with HttpClient.Http4s
            with ReleaseChecker.Live {
              override def subscribers: Ref[SubscriberMap]         = subscriberMap
              override def subscriptions: Ref[SubscriptionMap]     = subscriptionMap
              override def client: Client[Task]                    = http4sClient
              override implicit def canoeClient: CanoeClient[Task] = globalCanoeClient
          }
        }
      }
    }
  }
}
