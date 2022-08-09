package io.pager.lookup

import io.pager.PagerError
import io.pager.client.github.GitHubClient
import io.pager.client.telegram.TelegramClient
import io.pager.log.Logger
import io.pager.subscription.Repository.{ Name, Version }
import io.pager.subscription.{ Repository, SubscriptionLogic }
import zio.{ IO, Task, ZIO }

import scala.util.Try

private[lookup] final case class Live(
  logger: Logger,
  gitHubClient: GitHubClient,
  telegramClient: TelegramClient,
  subscriptionLogic: SubscriptionLogic
) extends ReleaseChecker {
  override def scheduleRefresh: Task[Unit] =
    for {
      _              <- logger.info("Getting latest repository versions")
      repos          <- subscriptionLogic.listRepositories
      latestVersions <- latestRepositoryVersions(repos.keySet)
      updatedVersions = newVersions(repos, latestVersions)
      _              <- subscriptionLogic.updateVersions(updatedVersions)
      statuses       <- repositoryStates(updatedVersions)
      _              <- broadcastUpdates(statuses)
      _              <- logger.info("Finished repository refresh")
    } yield ()

  private def repositoryStates(updatedVersions: Map[Name, Version]): Task[List[Repository]] =
    ZIO.foreach(updatedVersions.toList) { case (name, version) =>
      subscriptionLogic
        .listSubscribers(name)
        .map(subscribers => Repository(name, version, subscribers))
    }

  private def latestRepositoryVersions(repos: Set[Name]): IO[PagerError, Map[Name, Option[Version]]] =
    ZIO
      .foreach(repos) { name =>
        gitHubClient
          .releases(name)
          .map(releases => name -> Try(releases.maxBy(_.published_at).name).toOption)
      }
      .map(_.toMap)

  private def newVersions(
    latestKnownReleases: Map[Name, Option[Version]],
    latestReleases: Map[Name, Option[Version]]
  ): Map[Name, Version] =
    latestKnownReleases.flatMap { case (name, latestKnownVersion) =>
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
