package io.pager.logger

import io.pager.PagerError
import zio.ZIO

trait Logger {
  val logger: Logger.Service
}

object Logger {

  trait Service {
    def trace(message: => String): ZIO[Any, PagerError, Unit]

    def debug(message: => String): ZIO[Any, PagerError, Unit]

    def info(message: => String): ZIO[Any, PagerError, Unit]

    def warn(message: => String): ZIO[Any, PagerError, Unit]

    def error(message: => String): ZIO[Any, PagerError, Unit]

    def error(t: Throwable)(message: => String): ZIO[Any, PagerError, Unit]
  }
}
