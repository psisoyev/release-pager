package io

import io.pager.api.github.GitHubClient
import io.pager.api.http.HttpClient
import io.pager.api.telegram.TelegramClient
import io.pager.logging.Logger
import io.pager.storage.SubscriptionRepository
import io.pager.validation.RepositoryValidator
import zio.clock.Clock

package object pager {
  type LoggingEnv    = Logger
  type ValidationEnv = RepositoryValidator with HttpClient with LoggingEnv
  type ClientEnv     = SubscriptionRepository with ValidationEnv
  type AppEnv        = Clock with TelegramClient with ClientEnv with GitHubClient
}
