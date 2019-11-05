package io.pager.logging

import io.pager.PagerError
import zio.{ UIO, ZIO }

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

  object Test extends Service {
    def trace(message: => String): UIO[Unit] = UIO.unit

    def debug(message: => String): UIO[Unit] = UIO.unit

    def info(message: => String): UIO[Unit] = UIO.unit

    def warn(message: => String): UIO[Unit] = UIO.unit

    def error(message: => String): UIO[Unit] = UIO.unit

    def error(t: Throwable)(message: => String): UIO[Unit] = UIO.unit
  }
}
