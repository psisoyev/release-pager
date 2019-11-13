package io.pager.validation

import io.pager.PagerError
import io.pager.Subscription.RepositoryName
import io.pager.api.github.GitHubClient
import io.pager.logging.Logger
import zio.IO

trait GitHubRepositoryValidator extends RepositoryValidator {
  val logger: Logger.Service
  val gitHubClient: GitHubClient.Service

  def repositoryValidator: RepositoryValidator.Service = new RepositoryValidator.Service {
    def validate(name: String): IO[PagerError, RepositoryName] =
      gitHubClient
        .repositoryExists(RepositoryName(name))
        .foldM(
          e => logger.info(s"Failed to find repository $name") *> IO.fail(e),
          r => logger.info(s"Validated repository $name") *> IO.succeed(r)
        )
  }
}
