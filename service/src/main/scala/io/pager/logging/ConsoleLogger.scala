package io.pager.logging

import io.pager.ThrowableOps._
import zio.UIO
import zio.console.Console

trait ConsoleLogger extends Logger with Console {
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
