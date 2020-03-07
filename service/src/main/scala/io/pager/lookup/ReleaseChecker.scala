package io.pager.lookup

import io.pager.PagerError
import io.pager.client.github.GitHubClient
import io.pager.client.telegram.TelegramClient
import io.pager.logging._
import io.pager.subscription.Repository.{ Name, Version }
import io.pager.subscription.{ Repository, SubscriptionLogic }
import zio.{ Has, IO, Task, ZIO, ZLayer }

import scala.util.Try

object ReleaseChecker {
  trait Service {
    def scheduleRefresh: Task[Unit]
  }

  final case class Live(
    logger: Logger.Service,
    gitHubClient: GitHubClient.Service,
    telegramClient: TelegramClient.Service,
    subscriptionLogic: SubscriptionLogic.Service
  ) extends Service {
    override def scheduleRefresh: Task[Unit] =
      for {
        _               <- logger.info("Getting latest repository versions")
        repos           <- subscriptionLogic.listRepositories
        latestVersions  <- latestRepositoryVersions(repos.keySet)
        updatedVersions = newVersions(repos, latestVersions)
        _               <- subscriptionLogic.updateVersions(updatedVersions)
        statuses        <- repositoryStates(updatedVersions)
        _               <- broadcastUpdates(statuses)
        _               <- logger.info("Finished repository refresh")
      } yield ()

    private def repositoryStates(updatedVersions: Map[Name, Version]): Task[List[Repository]] =
      ZIO.foreach(updatedVersions) {
        case (name, version) =>
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

  val live
    : ZLayer[Has[Logger.Service] with Has[GitHubClient.Service] with Has[TelegramClient.Service] with Has[SubscriptionLogic.Service], Nothing, Has[
      Service
    ]] =
    ZLayer.fromServices[Logger.Service, GitHubClient.Service, TelegramClient.Service, SubscriptionLogic.Service, ReleaseChecker.Service] {
      (logger: Logger.Service, gc: GitHubClient.Service, tc: TelegramClient.Service, sl: SubscriptionLogic.Service) =>
        ReleaseChecker.Live(logger, gc, tc, sl)
    }
}
