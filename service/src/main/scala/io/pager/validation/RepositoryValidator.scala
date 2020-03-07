package io.pager.validation

import io.pager.PagerError
import io.pager.client.github.GitHubClient
import io.pager.logging.Logger
import io.pager.subscription.Repository.Name
import zio.{ Has, IO, ZLayer }

object RepositoryValidator {
  type RepositoryValidator = Has[Service]

  trait Service {
    def validate(text: String): IO[PagerError, Name]
  }

  case class GitHub(logger: Logger.Service, gitHubClient: GitHubClient.Service) extends Service {
    def validate(name: String): IO[PagerError, Name] =
      gitHubClient
        .repositoryExists(Name(name))
        .foldM(
          e => logger.info(s"Failed to find repository $name") *> IO.fail(e),
          r => logger.info(s"Validated repository $name") *> IO.succeed(r)
        )
  }

  val gitHub: ZLayer[Any, Nothing, Has[GitHub]] =
    ZLayer.fromFunction { (logger: Logger.Service, gitHubClient: GitHubClient.Service) =>
      GitHub(logger, gitHubClient)
    }
}
