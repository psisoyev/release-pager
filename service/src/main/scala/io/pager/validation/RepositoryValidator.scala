package io.pager.validation

import io.pager.{ PagerError, Subscription }
import zio.ZIO

trait RepositoryValidator {
  def repositoryValidator: RepositoryValidator.Service
}

object RepositoryValidator {
  trait Service {
    def validate(text: String): ZIO[Any, PagerError, Subscription.RepositoryName]
  }
}
