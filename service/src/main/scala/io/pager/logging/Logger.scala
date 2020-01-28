package io.pager.logging

import io.pager.ThrowableOps._
import zio.UIO
import zio.clock._
import zio.console.{ Console => ConsoleZIO }

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

  trait Console extends Logger with ConsoleZIO with Clock {
    val logger: Logger.Service = new Logger.Service {
      def error(message: => String): UIO[Unit] = print(message)

      def warn(message: => String): UIO[Unit] = print(message)

      def info(message: => String): UIO[Unit] = print(message)

      def debug(message: => String): UIO[Unit] = print(message)

      def trace(message: => String): UIO[Unit] = print(message)

      def error(t: Throwable)(message: => String): UIO[Unit] =
        for {
          _ <- print(message)
          _ <- console.putStrLn(t.stackTrace)
        } yield ()

      private def print(message: => String): UIO[Unit] =
        for {
          timestamp <- clock.currentDateTime
          _         <- console.putStrLn(s"[$timestamp] $message")
        } yield ()
    }
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
