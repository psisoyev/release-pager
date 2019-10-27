package io.pager

import zio.ZIO

package object validation {
  def validate(text: String): AppTask[Subscription.RepositoryUrl] = ZIO.accessM[AppEnv](_.validator.validate(text))
}
