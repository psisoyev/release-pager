package io.pager.lookup

import io.pager.PagerError
import io.pager.client.github.GitHubClient
import io.pager.client.telegram.TelegramClient
import io.pager.logging._
import io.pager.subscription.RepositoryStatus.Version
import io.pager.subscription.{RepositoryName, RepositoryStatus, SubscriptionLogic}
import zio.{IO, Task, UIO, ZIO}

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
          statuses        <- repositoryStatuses(updatedVersions)
          _               <- subscription.updateVersions(updatedVersions)
          _               <- broadcastUpdates(statuses)
          _               <- logger.info("Finished repository refresh")
        } yield ()
    }

    private def repositoryStatuses(updatedVersions: Map[RepositoryName, Version]): UIO[Map[RepositoryName, RepositoryStatus]] =
      ZIO
        .foreach(updatedVersions) {
          case (name, version) =>
            subscription
              .listSubscribers(name)
              .map(subscribers => name -> RepositoryStatus(version, subscribers))
        }
        .map(_.toMap)

    private def latestRepositoryVersions(repos: Set[RepositoryName]): IO[PagerError, Map[RepositoryName, Option[Version]]] =
      ZIO
        .traverse(repos) { repositoryName =>
          gitHubClient
            .releases(repositoryName)
            .map(releases => repositoryName -> Try(releases.maxBy(_.published_at).name).toOption)
        }
        .map(_.toMap)

    private def newVersions(
      latestKnownReleases: Map[RepositoryName, Option[Version]],
      latestReleases: Map[RepositoryName, Option[Version]]
    ): Map[RepositoryName, Version] =
      latestKnownReleases.flatMap {
        case (name, latestKnownVersion) =>
          latestReleases
            .get(name)
            .collect { case Some(latestVersion) if !latestKnownVersion.contains(latestVersion) => name -> latestVersion }
      }

    private def broadcastUpdates(repos: Map[RepositoryName, RepositoryStatus]): Task[Unit] =
      ZIO
        .foreach(repos)(telegramClient.broadcastNewVersion _ tupled)
        .unit
  }
}
