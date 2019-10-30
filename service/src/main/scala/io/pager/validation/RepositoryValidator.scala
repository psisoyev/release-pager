package io.pager.validation

import io.pager.api.http.HttpClient
import io.pager.logger.Logger
import io.pager.{ PagerError, Subscription }
import zio.ZIO

trait RepositoryValidator {
  def validator: RepositoryValidator.Service
}

object RepositoryValidator {
  type ValidatorEnv = Logger with HttpClient

  trait Service {
    def validate(text: String): ZIO[ValidatorEnv, PagerError, Subscription.RepositoryUrl]
  }
}
