package io.pager.log

import zio.{ UIO, ZIO }

private[log] case object Silent extends Logger {
  def trace(message: => String): UIO[Unit]               = ZIO.unit
  def debug(message: => String): UIO[Unit]               = ZIO.unit
  def info(message: => String): UIO[Unit]                = ZIO.unit
  def warn(message: => String): UIO[Unit]                = ZIO.unit
  def error(message: => String): UIO[Unit]               = ZIO.unit
  def error(t: Throwable)(message: => String): UIO[Unit] = ZIO.unit
}
