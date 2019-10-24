package io

import io.pager.api.telegram.TelegramClient
import io.pager.logger.Logger
import io.pager.validation.RepositoryValidator
import zio.RIO
import zio.clock.Clock
import zio.console.Console

package object pager {
  type AppEnv = Clock with Console with Logger with RepositoryValidator with TelegramClient
  type AppTask[A] = RIO[AppEnv, A]
}
