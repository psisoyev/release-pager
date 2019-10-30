package io.pager.validation

import io.pager.{ PagerError, Subscription }
import zio.ZIO

trait RepositoryValidator {
  def validator: RepositoryValidator.Service[Any]
}

object RepositoryValidator {
  trait Service[R] {
    def validate(text: String): ZIO[R, PagerError, Subscription.RepositoryUrl]
  }
}
