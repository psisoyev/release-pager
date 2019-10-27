package io

import io.pager.api.http.HttpClient
import io.pager.api.telegram.TelegramClient
import io.pager.logger.Logger
import io.pager.storage.SubscriptionRepository
import io.pager.validation.RepositoryValidator
import zio.RIO
import zio.clock.Clock
import zio.console.Console

package object pager {
  type AppEnv     = Clock with Console with Logger with RepositoryValidator with TelegramClient with HttpClient with SubscriptionRepository
  type AppTask[A] = RIO[AppEnv, A]
}
