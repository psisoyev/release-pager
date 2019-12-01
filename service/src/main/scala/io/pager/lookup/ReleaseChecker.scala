package io.pager.lookup

import io.pager.PagerError
import io.pager.client.github.GitHubClient
import io.pager.client.telegram.TelegramClient
import io.pager.logging._
import io.pager.subscription.Repository.{ Name, Version }
import io.pager.subscription.{ Repository, SubscriptionLogic }
import zio.{ IO, Task, ZIO }

import scala.util.Try

trait ReleaseChecker {
  val releaseChecker: ReleaseChecker.Service
}

object ReleaseChecker {
  trait Service {
    def scheduleRefresh: Task[Unit]
  }

  trait Live extends ReleaseChecker {
    def logger: Logger.Service
    def gitHubClient: GitHubClient.Service
    def telegramClient: TelegramClient.Service
    def subscription: SubscriptionLogic.Service

    override val releaseChecker: Service = new Service {

      override def scheduleRefresh: Task[Unit] =
        for {
          _               <- logger.info("Getting latest repository versions")
          repos           <- subscription.listRepositories
          latestVersions  <- latestRepositoryVersions(repos.keySet)
          updatedVersions = newVersions(repos, latestVersions)
          _               <- subscription.updateVersions(updatedVersions)
          statuses        <- repositoryStates(updatedVersions)
          _               <- broadcastUpdates(statuses)
          _               <- logger.info("Finished repository refresh")
        } yield ()
    }

    private def repositoryStates(updatedVersions: Map[Name, Version]): Task[List[Repository]] =
      ZIO
        .foreach(updatedVersions) {
          case (name, version) =>
            subscription
              .listSubscribers(name)
              .map(subscribers => Repository(name, version, subscribers))
        }

    private def latestRepositoryVersions(repos: Set[Name]): IO[PagerError, Map[Name, Option[Version]]] =
      ZIO
        .traverse(repos) { name =>
          gitHubClient
            .releases(name)
            .map(releases => name -> Try(releases.maxBy(_.published_at).name).toOption)
        }
        .map(_.toMap)

    private def newVersions(
      latestKnownReleases: Map[Name, Option[Version]],
      latestReleases: Map[Name, Option[Version]]
    ): Map[Name, Version] =
      latestKnownReleases.flatMap {
        case (name, latestKnownVersion) =>
          latestReleases
            .get(name)
            .collect { case Some(latestVersion) if !latestKnownVersion.contains(latestVersion) => name -> latestVersion }
      }

    private def broadcastUpdates(repos: List[Repository]): Task[Unit] =
      ZIO
        .foreach(repos) { repo =>
          val message = s"There is a new version of ${repo.name.value} available: ${repo.version.value}"
          telegramClient.broadcastMessage(repo.subscribers, message)
        }
        .unit
  }
}
