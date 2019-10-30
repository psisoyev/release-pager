package io.pager.logger

import io.pager.PagerError
import io.pager.ThrowableOps._
import zio.console.Console
import zio.{ console, ZIO }

trait ConsoleLogger extends Logger.Service[Console] {
  def error(message: => String): ZIO[Console, PagerError, Unit] = console.putStrLn(message)

  def warn(message: => String): ZIO[Console, PagerError, Unit] = console.putStrLn(message)

  def info(message: => String): ZIO[Console, PagerError, Unit] = console.putStrLn(message)

  def debug(message: => String): ZIO[Console, PagerError, Unit] = console.putStrLn(message)

  def trace(message: => String): ZIO[Console, PagerError, Unit] = console.putStrLn(message)

  def error(t: Throwable)(message: => String): ZIO[Console, PagerError, Unit] =
    for {
      _ <- console.putStrLn(t.stackTrace)
      _ <- console.putStrLn(message)
    } yield ()
}
