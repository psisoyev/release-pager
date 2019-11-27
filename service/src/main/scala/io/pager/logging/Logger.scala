package io.pager.logging

import zio.UIO

trait Logger {
  val logger: Logger.Service
}

object Logger {

  trait Service {
    def trace(message: => String): UIO[Unit]

    def debug(message: => String): UIO[Unit]

    def info(message: => String): UIO[Unit]

    def warn(message: => String): UIO[Unit]

    def error(message: => String): UIO[Unit]

    def error(t: Throwable)(message: => String): UIO[Unit]
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
