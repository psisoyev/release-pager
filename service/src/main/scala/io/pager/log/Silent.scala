package io.pager.log

import zio.UIO

private[log] case object Silent extends Logger.Service {
  def trace(message: => String): UIO[Unit]               = UIO.unit
  def debug(message: => String): UIO[Unit]               = UIO.unit
  def info(message: => String): UIO[Unit]                = UIO.unit
  def warn(message: => String): UIO[Unit]                = UIO.unit
  def error(message: => String): UIO[Unit]               = UIO.unit
  def error(t: Throwable)(message: => String): UIO[Unit] = UIO.unit
}
