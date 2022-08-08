package io.pager.log

import zio.{ UIO, ULayer, ZLayer }

trait Logger {
  def trace(message: => String): UIO[Unit]

  def debug(message: => String): UIO[Unit]

  def info(message: => String): UIO[Unit]

  def warn(message: => String): UIO[Unit]

  def error(message: => String): UIO[Unit]

  def error(t: Throwable)(message: => String): UIO[Unit]
}

object Logger {
  def console: ULayer[Logger] = ZLayer.succeed(Console())
  def silent: ULayer[Logger]  = ZLayer.succeed(Silent)
}
