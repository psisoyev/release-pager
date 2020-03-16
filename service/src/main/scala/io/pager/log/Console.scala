package io.pager.log

import zio.clock._
import zio.console.{ Console => ConsoleZIO }
import zio.UIO
import io.pager.ThrowableOps._

private[log] final case class Console(clock: Clock.Service, console: ConsoleZIO.Service) extends Logger.Service {
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
      timestamp <- clock.currentDateTime.orDie
      _         <- console.putStrLn(s"[$timestamp] $message")
    } yield ()
}
