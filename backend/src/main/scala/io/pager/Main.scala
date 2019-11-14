package io.pager

import java.util.concurrent.TimeUnit

import canoe.api.{ TelegramClient => CanoeClient }
import cats.effect.Resource
import io.pager.Subscription.{ ChatId, RepositoryName }
import io.pager.api.github.GitHubClient
import io.pager.api.http.HttpClient
import io.pager.api.telegram.TelegramClient
import io.pager.logging._
import io.pager.lookup.ReleaseChecker
import io.pager.subscription.{ InMemorySubscriptionRepository, SubscriptionRepository }
import io.pager.validation.{ GitHubRepositoryValidator, RepositoryValidator }
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import zio.clock.Clock
import zio.console.{ putStrLn, Console }
import zio.duration.Duration
import zio.interop.catz._
import zio.{ ZEnv, _ }

import scala.concurrent.ExecutionContext.Implicits

object Main extends zio.App {
  type AppEnv = SubscriptionRepository
    with RepositoryValidator
    with HttpClient
    with Logger
    with TelegramClient
    with GitHubClient
    with Clock
    with ReleaseChecker

  override def run(args: List[String]): ZIO[ZEnv, Nothing, Int] = {
    val token = "XXX"

    val program = for {
      _ <- putStrLn("Starting bot")

      subscriberMap   <- Ref.make(Map.empty[RepositoryName, RepositoryStatus])
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
    subscriberMap: Ref[Map[RepositoryName, RepositoryStatus]],
    subscriptionMap: Ref[Map[ChatId, Set[RepositoryName]]],
    http4sClient: Resource[Task, Client[Task]],
    canoeClient: Resource[Task, CanoeClient[Task]]
  ): Task[Int] = {
    val scheduleReleaseChecker = ZIO.accessM[AppEnv](_.releaseChecker.scheduleRefresh).repeat(ZSchedule.fixed(Duration(10, TimeUnit.SECONDS)))
    val startTelegramClient    = ZIO.accessM[TelegramClient](_.telegramClient.start).fork
    val program                = startTelegramClient *> scheduleReleaseChecker

    canoeClient.use { globalCanoeClient =>
      http4sClient.use { http4sClient =>
        program.provide {
          new TelegramClient.Canoe with Clock.Live with Console.Live with ConsoleLogger with InMemorySubscriptionRepository
          with GitHubRepositoryValidator with GitHubClient.Live with HttpClient.Http4s with ReleaseChecker.Live {
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
