package io.pager

import java.util.concurrent.TimeUnit

import canoe.api.{ TelegramClient => CanoeClient }
import cats.effect.Resource
import io.pager.client.github.GitHubClient
import io.pager.client.http.HttpClient
import io.pager.client.telegram.{ ChatId, TelegramClient }
import io.pager.logging._
import io.pager.lookup.ReleaseChecker
import io.pager.subscription.ChatStorage.SubscriptionMap
import io.pager.subscription.RepositoryStatus.Version
import io.pager.subscription.RepositoryVersionStorage.SubscriberMap
import io.pager.subscription.{ ChatStorage, RepositoryName, RepositoryVersionStorage, SubscriptionLogic }
import io.pager.validation.{ GitHubRepositoryValidator, RepositoryValidator }
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import zio._
import zio.clock.Clock
import zio.console.{ putStrLn, Console }
import zio.duration.Duration
import zio.interop.catz._

import scala.concurrent.ExecutionContext.Implicits

object Main extends zio.App {
  type AppEnv = SubscriptionLogic
    with RepositoryValidator
    with HttpClient
    with Logger
    with TelegramClient
    with GitHubClient
    with Clock
    with ReleaseChecker

  override def run(args: List[String]): ZIO[ZEnv, Nothing, Int] = {
    val token = sys.env("BOT_TOKEN")

    val program = for {
      subscriberMap   <- Ref.make(Map.empty[RepositoryName, Option[Version]])
      subscriptionMap <- Ref.make(Map.empty[ChatId, Set[RepositoryName]])

      http4sClient <- buildHttpClient
      canoeClient  <- buildTelegramClient(token)

      _ <- startProgram(subscriberMap, subscriptionMap, http4sClient, canoeClient)
    } yield ()

    program.foldM(
      err => putStrLn(s"Execution failed with: $err") *> ZIO.succeed(1),
      _ => ZIO.succeed(0)
    )
  }

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

  private def startProgram(
    subscriberMap: Ref[Map[RepositoryName, Option[Version]]],
    subscriptionMap: Ref[Map[ChatId, Set[RepositoryName]]],
    http4sClient: Resource[Task, Client[Task]],
    canoeClient: Resource[Task, CanoeClient[Task]]
  ): Task[Int] = {
    val scheduleReleaseChecker = ZIO.accessM[AppEnv](_.releaseChecker.scheduleRefresh).repeat(ZSchedule.fixed(Duration(1, TimeUnit.MINUTES)))
    val startTelegramClient    = ZIO.accessM[TelegramClient](_.telegramClient.start).fork
    val program                = startTelegramClient *> scheduleReleaseChecker

    canoeClient.use { globalCanoeClient =>
      http4sClient.use { http4sClient =>
        program.provide {
          new TelegramClient.Canoe with Clock.Live with Console.Live with ConsoleLogger with SubscriptionLogic.Live with ChatStorage.InMemory
          with RepositoryVersionStorage.InMemory with GitHubRepositoryValidator with GitHubClient.Live with HttpClient.Http4s
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
