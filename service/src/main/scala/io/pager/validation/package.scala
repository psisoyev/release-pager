package io.pager

import zio.ZIO

package object validation {
  def validate(text: String): ZIO[RepositoryValidator with ValidatorEnv, PagerError, Subscription.RepositoryUrl] =
    ZIO.accessM[RepositoryValidator with ValidatorEnv](_.validator.validate(text))
}
