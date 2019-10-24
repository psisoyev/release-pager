package io.pager.logger

import io.pager.PagerError
import io.pager.ThrowableOps._
import zio.ZIO
import zio.console.Console

trait ConsoleLogger extends Logger { self: Console =>
  override val logger: Logger.Service = new Logger.Service {
    def error(message: => String): ZIO[Any, PagerError, Unit] = console.putStrLn(message)

    def warn(message: => String): ZIO[Any, PagerError, Unit] = console.putStrLn(message)

    def info(message: => String): ZIO[Any, PagerError, Unit] = console.putStrLn(message)

    def debug(message: => String): ZIO[Any, PagerError, Unit] = console.putStrLn(message)

    def trace(message: => String): ZIO[Any, PagerError, Unit] = console.putStrLn(message)

    def error(t: Throwable)(message: => String): ZIO[Any, PagerError, Unit] = for {
      _ <- console.putStrLn(t.stackTrace)
      _ <- console.putStrLn(message)
    } yield ()
  }
}