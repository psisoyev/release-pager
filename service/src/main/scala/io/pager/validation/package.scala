package io.pager

import zio.ZIO

package object validation {
  def validate(text: String): ZIO[RepositoryValidator, PagerError, Subscription.RepositoryUrl] =
    ZIO.accessM[RepositoryValidator](_.validator.validate(text))
}
