package io.pager.lookup

import io.pager.Generators._
import io.pager.PagerError.NotFound
import io.pager.TestData._
import io.pager.client.github.GitHubClient.GitHubClient
import io.pager.client.github.{ GitHubClient, GitHubClientMock }
import io.pager.client.telegram.TelegramClient.TelegramClient
import io.pager.client.telegram.{ ChatId, TelegramClient, TelegramClientMock }
import io.pager.log.Logger
import io.pager.subscription.Repository.{ Name, Version }
import io.pager.subscription.SubscriptionLogic.SubscriptionLogic
import io.pager.subscription.SubscriptionLogicMock
import zio._
import zio.test.Assertion._
import zio.test.mock.Expectation._
import zio.test.{ DefaultRunnableSpec, _ }

object LiveReleaseCheckerSpec extends DefaultRunnableSpec {
  override def spec: ZSpec[Environment, Failure] = suite("LiveReleaseCheckerSpec")(
    testM("Do not call services if there are no repositories") {
      val gitHubClientMocks: ULayer[GitHubClient]           = GitHubClient.empty
      val telegramClientMocks: ULayer[TelegramClient]       = TelegramClient.empty
      val subscriptionLogicMocks: ULayer[SubscriptionLogic] =
        SubscriptionLogicMock.ListRepositories(value(Map.empty[Name, Option[Version]])) ++
          SubscriptionLogicMock.UpdateVersions(equalTo(Map.empty[Name, Version]), unit)

      scheduleRefresh(gitHubClientMocks, telegramClientMocks, subscriptionLogicMocks)
    },
    testM("Do not bother subscribers if there are no version updates") {
      checkM(repositoryName) { name =>
        val repositories = Map(name -> Some(finalVersion))

        val gitHubClientMocks: ULayer[GitHubClient]           = GitHubClientMock.Releases(equalTo(name), value(List(finalRelease)))
        val telegramClientMocks: ULayer[TelegramClient]       = TelegramClient.empty
        val subscriptionLogicMocks: ULayer[SubscriptionLogic] =
          SubscriptionLogicMock.ListRepositories(value(repositories)) ++
            SubscriptionLogicMock.UpdateVersions(equalTo(Map.empty[Name, Version]), unit)

        scheduleRefresh(gitHubClientMocks, telegramClientMocks, subscriptionLogicMocks)
      }
    },
    testM("Update repository version for the very first time") {
      checkM(repositoryName) { name =>
        val repositories = Map(name -> None)

        val gitHubClientMocks      = GitHubClientMock.Releases(equalTo(name), value(List(finalRelease)))
        val telegramClientMocks    = TelegramClientMock.BroadcastMessage(equalTo(Set.empty[ChatId], message(name)), unit)
        val subscriptionLogicMocks =
          SubscriptionLogicMock.ListRepositories(value(repositories)) ++
            SubscriptionLogicMock.UpdateVersions(equalTo(Map(name -> finalVersion)), unit) ++
            SubscriptionLogicMock.ListSubscribers(equalTo(name), value(Set.empty))

        scheduleRefresh(gitHubClientMocks, telegramClientMocks, subscriptionLogicMocks)
      }
    },
    testM("Notify users about new release") {
      checkM(repositoryName, chatIds) { case (name, (chatId1, chatId2)) =>
        val repositories = Map(name -> Some(rcVersion))
        val subscribers  = Set(chatId1, chatId2)

        val gitHubClientMocks      = GitHubClientMock.Releases(equalTo(name), value(releases))
        val telegramClientMocks    = TelegramClientMock.BroadcastMessage(equalTo((subscribers, message(name))), unit)
        val subscriptionLogicMocks =
          SubscriptionLogicMock.ListRepositories(value(repositories)) ++
            SubscriptionLogicMock.UpdateVersions(equalTo(Map(name -> finalVersion)), unit) ++
            SubscriptionLogicMock.ListSubscribers(equalTo(name), value(subscribers))

        scheduleRefresh(gitHubClientMocks, telegramClientMocks, subscriptionLogicMocks)
      }
    },
    testM("GitHub client error should be handled") {
      checkM(repositoryName) { name =>
        val repositories           = Map(name -> Some(rcVersion))
        val error                  = NotFound(name.value)
        val gitHubClientMocks      = GitHubClientMock.Releases(equalTo(name), failure(error))
        val telegramClientMocks    = TelegramClient.empty
        val subscriptionLogicMocks = SubscriptionLogicMock.ListRepositories(value(repositories))

        val result = scheduleRefresh(gitHubClientMocks, telegramClientMocks, subscriptionLogicMocks)
        assertM(result.run)(fails(equalTo(error)))
      }
    }
  )

  private def scheduleRefresh(
    gitHubClient: ULayer[GitHubClient],
    telegramClient: ULayer[TelegramClient],
    subscriptionLogic: ULayer[SubscriptionLogic]
  ): ZIO[ZEnv, Throwable, TestResult] = {
    val layer = (Logger.silent ++ gitHubClient ++ telegramClient ++ subscriptionLogic) >>> ReleaseChecker.live

    ReleaseChecker
      .scheduleRefresh
      .provideLayer(layer)
      .as(assertCompletes)
  }
}
