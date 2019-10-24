package io.pager

import zio.ZIO

package object validation {
  def validate(text: String): AppTask[Repository.Name] = ZIO.accessM[AppEnv](_.validator.validate(text))
}
