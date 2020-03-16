package io.pager.validation

import io.pager.log.Logger
import io.pager.client.github.GitHubClient
import io.pager.validation.RepositoryValidator
import zio.IO
import io.pager.subscription.Repository.Name
import io.pager.PagerError

private[validation] final case class GitHub(
  logger: Logger.Service,
  gitHubClient: GitHubClient.Service
) extends RepositoryValidator.Service {
  def validate(name: String): IO[PagerError, Name] =
    gitHubClient
      .repositoryExists(Name(name))
      .foldM(
        e => logger.info(s"Failed to find repository $name") *> IO.fail(e),
        r => logger.info(s"Validated repository $name") *> IO.succeed(r)
      )
}
