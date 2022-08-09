package io.pager.log

import io.pager.ThrowableOps._
import zio.UIO

private[log] final case class Console() extends Logger {
  def error(message: => String): UIO[Unit] = print(message)

  def warn(message: => String): UIO[Unit] = print(message)

  def info(message: => String): UIO[Unit] = print(message)

  def debug(message: => String): UIO[Unit] = print(message)

  def trace(message: => String): UIO[Unit] = print(message)

  def error(t: Throwable)(message: => String): UIO[Unit] =
    for {
      _ <- print(message)
      _ <- zio.Console.printLine(t.stackTrace).orDie
    } yield ()

  private def print(message: => String): UIO[Unit] =
    (for {
      timestamp <- zio.Clock.currentDateTime
      _         <- zio.Console.printLine(s"[$timestamp] $message")
    } yield ()).orDie
}
