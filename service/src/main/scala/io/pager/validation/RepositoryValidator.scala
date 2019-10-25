package io.pager.validation

import io.pager.{ AppEnv, PagerError, Repository }
import zio.ZIO

trait RepositoryValidator {
  def validator: RepositoryValidator.Service
}

object RepositoryValidator {
  trait Service {
    def validate(text: String): ZIO[AppEnv, PagerError, Repository.Name]
  }
}
