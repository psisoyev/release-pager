package io

import io.pager.api.http.HttpClient
import io.pager.api.telegram.TelegramClient
import io.pager.logging.ConsoleLogger
import io.pager.storage.InMemorySubscriptionRepository
import io.pager.validation.GitHubRepositoryValidator
import zio.clock.Clock
import zio.console.Console

package object pager {
  type LoggerEnv     = Console
  type LoggingEnv    = ConsoleLogger with LoggerEnv
  type ValidatorEnv  = LoggingEnv with HttpClient
  type ValidationEnv = GitHubRepositoryValidator with ValidatorEnv
  type ClientEnv     = InMemorySubscriptionRepository with ValidationEnv
  type AppEnv        = Clock with TelegramClient.Canoe with ClientEnv with ValidatorEnv
}
