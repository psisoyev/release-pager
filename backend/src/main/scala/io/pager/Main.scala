package io.pager

import cats.effect.Resource
import io.pager.Subscription.{ ChatId, RepositoryUrl }
import io.pager.api.http.HttpClient
import io.pager.api.telegram.TelegramClient
import io.pager.logging._
import io.pager.storage.InMemorySubscriptionRepository
import io.pager.validation.GitHubRepositoryValidator
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import zio.clock.Clock
import zio.console.{ putStrLn, Console }
import zio.{ ZEnv, _ }
import zio.interop.catz._

import scala.concurrent.ExecutionContext.Implicits

object Main extends zio.App {
  override def run(args: List[String]): ZIO[ZEnv, Nothing, Int] = {
    val token = "XXX"

    val result: ZIO[ZEnv, Throwable, Unit] = for {
      _ <- putStrLn("Starting bot")

      program         = ZIO.environment[AppEnv].flatMap(_.telegramClient.start(token))
      subscriberMap   <- Ref.make(Map.empty[RepositoryUrl, RepositoryStatus])
      subscriptionMap <- Ref.make(Map.empty[ChatId, Set[RepositoryUrl]])
      http4sClient <- ZIO
                       .runtime[ZEnv]
                       .map { implicit rts =>
                         BlazeClientBuilder
                           .apply[Task](Implicits.global)
                           .resource
                       }

      _ <- putStrLn("Started bot")

      _ <- program.provide {
            new Clock.Live with Console.Live with ConsoleLogger with TelegramClient.Canoe with GitHubRepositoryValidator
            with InMemorySubscriptionRepository with HttpClient.Http4s with GitHubClient.Live {
              override def client: Resource[Task, Client[Task]] = http4sClient
              override def subscribers: Ref[SubscriberMap]      = subscriberMap
              override def subscriptions: Ref[SubscriptionMap]  = subscriptionMap
            }
          }
    } yield ()

    result.foldM(
      err => putStrLn(s"Execution failed with: $err") *> ZIO.succeed(1),
      _ => ZIO.succeed(0)
    )
  }
}
