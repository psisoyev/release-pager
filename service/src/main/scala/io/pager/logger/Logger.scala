package io.pager.logger

import io.pager.PagerError
import zio.ZIO

trait Logger {
  val logger: Logger.Service[Any]
}

object Logger {

  trait Service[R] {
    def trace(message: => String): ZIO[R, PagerError, Unit]

    def debug(message: => String): ZIO[R, PagerError, Unit]

    def info(message: => String): ZIO[R, PagerError, Unit]

    def warn(message: => String): ZIO[R, PagerError, Unit]

    def error(message: => String): ZIO[R, PagerError, Unit]

    def error(t: Throwable)(message: => String): ZIO[R, PagerError, Unit]
  }
}
