package io.pager.validation

import io.pager.PagerError
import io.pager.client.github.GitHubClient
import io.pager.log.Logger
import io.pager.subscription.Repository.Name
import zio.{ IO, ZIO }

private[validation] final case class GitHub(
  logger: Logger,
  gitHubClient: GitHubClient
) extends RepositoryValidator {
  def validate(name: String): IO[PagerError, Name] =
    gitHubClient
      .repositoryExists(Name(name))
      .foldZIO(
        e => logger.info(s"Failed to find repository $name") *> ZIO.fail(e),
        r => logger.info(s"Validated repository $name") *> ZIO.succeed(r)
      )
}
