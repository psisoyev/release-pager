package io.pager

import zio.ZIO

package object logging {
  def error(message: => String): ZIO[LoggingEnv, PagerError, Unit] = ZIO.accessM[LoggingEnv](_.error(message))

  def warn(message: => String): ZIO[LoggingEnv, PagerError, Unit] = ZIO.accessM[LoggingEnv](_.warn(message))

  def info(message: => String): ZIO[LoggingEnv, PagerError, Unit] = ZIO.accessM[LoggingEnv](_.info(message))

  def debug(message: => String): ZIO[LoggingEnv, PagerError, Unit] = ZIO.accessM[LoggingEnv](_.debug(message))

  def trace(message: => String): ZIO[LoggingEnv, PagerError, Unit] = ZIO.accessM[LoggingEnv](_.trace(message))

  def error(t: Throwable)(message: => String): ZIO[LoggingEnv, PagerError, Unit] =
    ZIO.accessM[LoggingEnv](_.error(t)(message))
}
