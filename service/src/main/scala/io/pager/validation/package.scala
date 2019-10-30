package io.pager

import zio.ZIO

package object validation {
  def validate(text: String): ZIO[GitHubRepositoryValidator with ValidatorEnv, PagerError, Subscription.RepositoryUrl] =
    ZIO.accessM[GitHubRepositoryValidator with ValidatorEnv](_.validate(text))
}
