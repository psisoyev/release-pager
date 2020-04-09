package io.pager.lookup

import io.pager.Generators._
import io.pager.PagerError.NotFound
import io.pager.TestData._
import io.pager.client.github.GitHubClient.GitHubClient
import io.pager.client.github.{ GitHubClient, GitHubClientMock }
import io.pager.client.telegram.TelegramClient.TelegramClient
import io.pager.client.telegram.{ ChatId, TelegramClient, TelegramClientMock }
import io.pager.log.Logger
import io.pager.log.Logger.Logger
import io.pager.subscription.Repository.{ Name, Version }
import io.pager.subscription.SubscriptionLogic.SubscriptionLogic
import io.pager.subscription.SubscriptionLogicMock
import zio._
import zio.test.Assertion._
import zio.test.mock.Expectation
import zio.test.mock.Expectation._
import zio.test.{ DefaultRunnableSpec, _ }

object LiveReleaseCheckerSpec extends DefaultRunnableSpec {
  override def spec: ZSpec[Environment, Failure] = suite("LiveReleaseCheckerSpec")(
    testM("Do not call services if there are no repositories") {
      val mocks =
        TelegramClient.empty ++
          GitHubClient.empty ++
          (SubscriptionLogicMock.ListRepositories(value(Map.empty[Name, Option[Version]])) &&
            SubscriptionLogicMock.UpdateVersions(equalTo(Map.empty[Name, Version]), unit))

      scheduleRefresh(mocks)
    },
    testM("Do not bother subscribers if there are no version updates") {
      checkM(repositoryName) { name =>
        val repositories = Map(name -> Some(finalVersion))
        val mocks =
          TelegramClient.empty ++
            GitHubClientMock.Releases(equalTo(name), value(List(finalRelease))) ++
            SubscriptionLogicMock.ListRepositories(value(repositories)) ++
            SubscriptionLogicMock.UpdateVersions(equalTo(Map.empty[Name, Version]))

        scheduleRefresh(mocks)
      }
    },
    testM("Update repository version for the very first time") {
      checkM(repositoryName) { name =>
        val repositories = Map(name -> None)
        val mocks =
          GitHubClientMock.Releases(equalTo(name), value(List(finalRelease))) ++
            TelegramClientMock.BroadcastMessage(equalTo(Set.empty[ChatId], message(name))) ++
            SubscriptionLogicMock.ListRepositories(value(repositories)) ++
            SubscriptionLogicMock.UpdateVersions(equalTo(Map(name -> finalVersion))) ++
            SubscriptionLogicMock.ListSubscribers(equalTo(name), value(Set.empty))

        scheduleRefresh(mocks)
      }
    },
    testM("Notify users about new release") {
      checkM(repositoryName, chatIds) {
        case (name, (chatId1, chatId2)) =>
          val repositories = Map(name -> Some(rcVersion))
          val subscribers  = Set(chatId1, chatId2)

          val mocks: Expectation[GitHubClient with TelegramClient with SubscriptionLogic] =
            GitHubClientMock.Releases(equalTo(name), value(releases)) &&
              TelegramClientMock.BroadcastMessage(equalTo((subscribers, message(name)))) &&
              SubscriptionLogicMock.ListRepositories(value(repositories)) &&
              SubscriptionLogicMock.UpdateVersions(equalTo(Map(name -> finalVersion))) &&
              SubscriptionLogicMock.ListSubscribers(equalTo(name), value(subscribers))

          scheduleRefresh(mocks)
      }
    },
    testM("GitHub client error should be handled") {
      checkM(repositoryName) { name =>
        val repositories = Map(name -> Some(rcVersion))
        val error        = NotFound(name.value)
        val mocks =
          TelegramClient.empty ++
            GitHubClientMock.Releases(equalTo(name), failure(error)) ++
            SubscriptionLogicMock.ListRepositories(value(repositories))

        assertM(scheduleRefresh(mocks).run)(fails(equalTo(error)))
      }
    }
  )

  private def scheduleRefresh(
    dependencies: ULayer[TelegramClient with GitHubClient with SubscriptionLogic]
  ): ZIO[ZEnv, Throwable, TestResult] = {

    val x: ZLayer[Any, Nothing, Logger with TelegramClient with GitHubClient with SubscriptionLogic] = Logger.silent ++ dependencies

    val layer: ZLayer[Any, Nothing, Has[ReleaseChecker.Service]] = x >>> ReleaseChecker.live

    ReleaseChecker
      .scheduleRefresh
      .provideLayer(layer)
      .as(assertCompletes)
  }
}
