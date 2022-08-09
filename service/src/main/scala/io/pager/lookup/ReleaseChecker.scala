package io.pager.lookup

import io.pager.client.github.GitHubClient
import io.pager.client.telegram.TelegramClient
import io.pager.log._
import io.pager.subscription.SubscriptionLogic
import zio._

trait ReleaseChecker {
  def scheduleRefresh: Task[Unit]
}

object ReleaseChecker {
  type LiveDeps = Logger with GitHubClient with TelegramClient with SubscriptionLogic
  val live: URLayer[LiveDeps, ReleaseChecker] =
    ZLayer {
      for {
        logger            <- ZIO.service[Logger]
        githubClient      <- ZIO.service[GitHubClient]
        telegramClient    <- ZIO.service[TelegramClient]
        subscriptionLogic <- ZIO.service[SubscriptionLogic]
      } yield Live(logger, githubClient, telegramClient, subscriptionLogic)
    }
}
