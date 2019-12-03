package io.pager.logging

import zio.UIO
import zio.console.{Console => ConsoleZIO }
import io.pager.ThrowableOps._

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

  trait Console extends Logger with ConsoleZIO {
    val logger: Logger.Service = new Logger.Service {
      override def error(message: => String): UIO[Unit] = console.putStrLn(message)

      def warn(message: => String): UIO[Unit] = console.putStrLn(message)

      def info(message: => String): UIO[Unit] = console.putStrLn(message)

      def debug(message: => String): UIO[Unit] = console.putStrLn(message)

      def trace(message: => String): UIO[Unit] = console.putStrLn(message)

      def error(t: Throwable)(message: => String): UIO[Unit] =
        for {
          _ <- console.putStrLn(t.stackTrace)
          _ <- console.putStrLn(message)
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
