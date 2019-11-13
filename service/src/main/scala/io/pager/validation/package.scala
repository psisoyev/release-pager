package io.pager

import zio.ZIO

package object validation {
  def validate(text: String): ZIO[RepositoryValidator, PagerError, Subscription.RepositoryName] =
    ZIO.accessM[RepositoryValidator](_.repositoryValidator.validate(text))
}
