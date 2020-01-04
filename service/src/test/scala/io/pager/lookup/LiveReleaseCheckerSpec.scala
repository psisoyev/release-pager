package io.pager.lookup

import io.pager.Generators
import io.pager.PagerError.NotFound
import io.pager.TestData._
import io.pager.client.github.GitHubClient
import io.pager.client.telegram.TelegramClient
import io.pager.logging.Logger
import io.pager.lookup.LiveReleaseCheckerTestCases._
import io.pager.subscription.SubscriptionLogic
import zio._
import zio.test.Assertion._
import zio.test.{ DefaultRunnableSpec, _ }
import zio.test.mock.Expectation._
import zio.test.mock._

object LiveReleaseCheckerSpec extends DefaultRunnableSpec(suite(specName)(scenarios: _*))

object LiveReleaseCheckerTestCases {
  val specName: String = "LiveReleaseCheckerSpec"

  val scenarios = List(
    testM("Do not call services if there are no repositories") {
      val gitHubClientMocks   = Expectation.nothing[GitHubClient]
      val telegramClientMocks = Expectation.nothing[TelegramClient]
      val subscriptionMocks =
        (SubscriptionLogic.listRepositories returns value(Map.empty)) *>
          (SubscriptionLogic.updateVersions(equalTo(Map.empty)) returns unit)

      scheduleRefreshSpec(subscriptionMocks, telegramClientMocks, gitHubClientMocks)
    },
    testM("Do not bother subscribers if there are no version updates") {
      checkM(Generators.repositoryName) { name =>
        val repositories = Map(name -> Some(finalVersion))

        val gitHubClientMocks   = GitHubClient.releases(equalTo(name)) returns value(List(finalRelease))
        val telegramClientMocks = Expectation.nothing[TelegramClient]
        val subscriptionMocks =
          (SubscriptionLogic.listRepositories returns value(repositories)) *>
            (SubscriptionLogic.updateVersions(equalTo(Map.empty)) returns unit)

        scheduleRefreshSpec(subscriptionMocks, telegramClientMocks, gitHubClientMocks)
      }
    },
    testM("Update repository version for the very first time") {
      checkM(Generators.repositoryName) { name =>
        val repositories = Map(name -> None)

        val gitHubClientMocks   = GitHubClient.releases(equalTo(name)) returns value(List(finalRelease))
        val telegramClientMocks = TelegramClient.broadcastMessage(equalTo(Set.empty, message(name))) returns unit
        val subscriptionMocks =
          (SubscriptionLogic.listRepositories returns value(repositories)) *>
            (SubscriptionLogic.updateVersions(equalTo(Map(name -> finalVersion))) returns unit) *>
            (SubscriptionLogic.listSubscribers(equalTo(name)) returns value(Set.empty))

        scheduleRefreshSpec(subscriptionMocks, telegramClientMocks, gitHubClientMocks)
      }
    },
    testM("Notify users when new version has found") {
      checkM(Generators.repositoryName) { name =>
        val repositories = Map(name -> Some(rcVersion))

        val gitHubClientMocks   = GitHubClient.releases(equalTo(name)) returns value(releases)
        val telegramClientMocks = TelegramClient.broadcastMessage(equalTo((subscribers, message(name)))) returns unit
        val subscriptionMocks =
          (SubscriptionLogic.listRepositories returns value(repositories)) *>
            (SubscriptionLogic.updateVersions(equalTo(Map(name -> finalVersion))) returns unit) *>
            (SubscriptionLogic.listSubscribers(equalTo(name)) returns value(subscribers))

        scheduleRefreshSpec(subscriptionMocks, telegramClientMocks, gitHubClientMocks)
      }
    },
    testM("GitHub client error should be handled") {
      checkM(Generators.repositoryName) { name =>
        val repositories        = Map(name -> Some(rcVersion))
        val error               = NotFound(name.value)
        val gitHubClientMocks   = GitHubClient.releases(equalTo(name)) returns failure(error)
        val telegramClientMocks = Expectation.nothing[TelegramClient]
        val subscriptionMocks   = (SubscriptionLogic.listRepositories returns value(repositories))

        val result = scheduleRefreshSpec(subscriptionMocks, telegramClientMocks, gitHubClientMocks)
        assertM(result.run, fails(equalTo(error)))
      }
    }
  )

  def scheduleRefreshSpec(
    subscriptionMocks: UManaged[SubscriptionLogic],
    telegramClientMocks: UManaged[TelegramClient],
    gitHubClientMocks: UManaged[GitHubClient]
  ): Task[TestResult] = {
    val mocks = subscriptionMocks &&& telegramClientMocks &&& gitHubClientMocks

    val service = mocks.map {
      case ((sl, tc), gc) =>
        new ReleaseChecker.Live {
          override def logger: Logger.Service                            = Logger.Test
          override def gitHubClient: GitHubClient.Service[Any]           = gc.gitHubClient
          override def telegramClient: TelegramClient.Service[Any]       = tc.telegramClient
          override def subscriptionLogic: SubscriptionLogic.Service[Any] = sl.subscriptionLogic
        }
    }

    service
      .use(_.releaseChecker.scheduleRefresh)
      .as(assertCompletes)
  }
}
