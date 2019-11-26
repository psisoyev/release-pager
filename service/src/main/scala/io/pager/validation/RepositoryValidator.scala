package io.pager.validation

import io.pager.PagerError
import io.pager.subscription.RepositoryName
import zio.IO

trait RepositoryValidator {
  def repositoryValidator: RepositoryValidator.Service
}

object RepositoryValidator {
  trait Service {
    def validate(text: String): IO[PagerError, RepositoryName]
  }
}
