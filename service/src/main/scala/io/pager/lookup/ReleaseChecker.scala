package io.pager.lookup

import io.pager.client.github.GitHubClient
import io.pager.client.github.GitHubClient.GitHubClient
import io.pager.client.telegram.TelegramClient
import io.pager.client.telegram.TelegramClient.TelegramClient
import io.pager.log.Logger.Logger
import io.pager.log._
import io.pager.subscription.SubscriptionLogic
import io.pager.subscription.SubscriptionLogic.SubscriptionLogic
import zio._

object ReleaseChecker {
  type ReleaseChecker = Has[Service]

  trait Service {
    def scheduleRefresh: Task[Unit]
  }

  type LiveDeps = Logger with GitHubClient with TelegramClient with SubscriptionLogic
  def live: URLayer[LiveDeps, Has[Service]] =
    ZLayer.fromServices[Logger.Service, GitHubClient.Service, TelegramClient.Service, SubscriptionLogic.Service, ReleaseChecker.Service] {
      (logger, githubClient, telegramClient, subscriptionLogic) =>
        Live(logger, githubClient, telegramClient, subscriptionLogic)
    }

  def scheduleRefresh: ZIO[ReleaseChecker, Throwable, Unit] = ZIO.accessM(_.get.scheduleRefresh)
}
