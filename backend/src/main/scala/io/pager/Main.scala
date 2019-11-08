package io.pager

import canoe.api.{TelegramClient => CanoeClient}
import cats.effect.Resource
import io.pager.Subscription.{ChatId, RepositoryName}
import io.pager.api.github.GitHubClient
import io.pager.api.http.HttpClient
import io.pager.api.telegram.TelegramClient.{Canoe, ClientTask}
import io.pager.logging._
import io.pager.storage.InMemorySubscriptionRepository
import io.pager.validation.GitHubRepositoryValidator
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import zio.clock.Clock
import zio.console.{Console, putStrLn}
import zio.interop.catz._
import zio.{ZEnv, _}

import scala.concurrent.ExecutionContext.Implicits

object Main extends zio.App {
  override def run(args: List[String]): ZIO[ZEnv, Nothing, Int] = {
    val token = "XXX"

    val program = for {
      _ <- putStrLn("Starting bot")

      subscriberMap   <- Ref.make(Map.empty[RepositoryName, RepositoryStatus])
      subscriptionMap <- Ref.make(Map.empty[ChatId, Set[RepositoryName]])

      http4sClient <- buildHttpClient
      _ <- startProgram(subscriberMap, subscriptionMap, http4sClient, token)

      _ <- putStrLn("Started bot")

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

  private def startTelegramClient(client: CanoeClient[ClientTask]): ClientTask[Unit] =
    new Canoe()(client).telegramClient.start

  private def startProgram(
    subscriberMap: Ref[Map[RepositoryName, RepositoryStatus]],
    subscriptionMap: Ref[Map[ChatId, Set[RepositoryName]]],
    http4sClient: Resource[Task, Client[Task]],
    token: String
  ): Task[Unit] = {
    ZIO
      .runtime[ClientEnv]
      .map { implicit rts => CanoeClient.global[ClientTask](token) }
      .flatMap(_.use(startTelegramClient))
      .provide {
        new Clock.Live with Console.Live with ConsoleLogger with InMemorySubscriptionRepository with GitHubRepositoryValidator
          with GitHubClient.Live with HttpClient.Http4s {
          override def subscribers: Ref[SubscriberMap]      = subscriberMap
          override def subscriptions: Ref[SubscriptionMap]  = subscriptionMap
          override def client: Resource[Task, Client[Task]] = http4sClient
        }
      }
  }
}
