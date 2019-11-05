package io.pager.validation

import io.pager.PagerError
import io.pager.Subscription.RepositoryUrl
import io.pager.api.github.GitHubClient
import io.pager.logging.Logger
import zio.IO

trait GitHubRepositoryValidator extends RepositoryValidator {
  val logger: Logger.Service
  val gitHubClient: GitHubClient.Service

  def validator: RepositoryValidator.Service = new RepositoryValidator.Service {
    def validate(name: String): IO[PagerError, RepositoryUrl] = {
      val url =
        if (name.startsWith("https://github.com/") || name.startsWith("http://github.com/")) name
        else s"https://github.com/$name"

      gitHubClient
        .repositoryExists(RepositoryUrl(url))
        .foldM(
          e => logger.info(s"Failed to find repository $name") *> IO.fail(e),
          r => logger.info(s"Validated repository $name") *> IO.succeed(r)
        )
    }
  }
}
