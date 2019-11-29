package io.pager.validation

import io.pager.PagerError
import io.pager.client.github.GitHubClient
import io.pager.logging.Logger
import io.pager.subscription.Repository.Name
import zio.IO

trait RepositoryValidator {
  def repositoryValidator: RepositoryValidator.Service
}

object RepositoryValidator {
  trait Service {
    def validate(text: String): IO[PagerError, Name]
  }

  trait GitHubRepositoryValidator extends RepositoryValidator {
    val logger: Logger.Service
    val gitHubClient: GitHubClient.Service

    def repositoryValidator: RepositoryValidator.Service = new RepositoryValidator.Service {
      def validate(name: String): IO[PagerError, Name] =
        gitHubClient
          .repositoryExists(Name(name))
          .foldM(
            e => logger.info(s"Failed to find repository $name") *> IO.fail(e),
            r => logger.info(s"Validated repository $name") *> IO.succeed(r)
          )
    }
  }
}
