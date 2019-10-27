package io.pager.validation

import io.pager.{ AppEnv, PagerError, Subscription }
import zio.ZIO

trait RepositoryValidator {
  def validator: RepositoryValidator.Service
}

object RepositoryValidator {
  trait Service {
    def validate(text: String): ZIO[AppEnv, PagerError, Subscription.RepositoryUrl]
  }
}
