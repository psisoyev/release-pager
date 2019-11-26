package io.pager.lookup

import io.pager.api.github.GitHubClient
import io.pager.api.telegram.TelegramClient
import io.pager.logging._
import io.pager.subscription.RepositoryStatus.Version
import io.pager.subscription.{ RepositoryName, RepositoryStatus, Subscription }
import zio.{ Task, ZIO }

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
    def subscription: Subscription.Service

    override val releaseChecker: Service = new Service {

      override def scheduleRefresh: Task[Unit] =
        for {
          _     <- logger.info("Getting latest repository versions")
          repos <- subscription.listRepositories
          _     <- broadcastNewVersions(repos)
          _     <- logger.info("Finished repository refresh")
        } yield ()
    }

    private def broadcastNewVersions(repos: Map[RepositoryName, RepositoryStatus]): Task[Unit] =
      ZIO
        .traverse(repos) {
          case (name, status) =>
            gitHubClient.releases(name).flatMap { releases =>
              val latest = releases.maxBy(_.published_at)
              telegramClient.broadcastNewVersion(name, status.copy(version = Some(Version(latest.name))))
            }
        }
        .unit
  }
}
