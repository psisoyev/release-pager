package io.pager.validation

import io.pager.PagerError
import io.pager.client.github.GitHubClient
import io.pager.logging.Logger
import io.pager.subscription.Repository.Name
import zio.{ Has, IO, ZIO, ZLayer }

object RepositoryValidator {
  type RepositoryValidator = Has[Service]

  trait Service {
    def validate(text: String): IO[PagerError, Name]
  }

  final case class GitHub(logger: Logger.Service, gitHubClient: GitHubClient.Service) extends Service {
    def validate(name: String): IO[PagerError, Name] =
      gitHubClient
        .repositoryExists(Name(name))
        .foldM(
          e => logger.info(s"Failed to find repository $name") *> IO.fail(e),
          r => logger.info(s"Validated repository $name") *> IO.succeed(r)
        )
  }

  private type LiveDeps = Has[Logger.Service] with Has[GitHubClient.Service]
  val live: ZLayer[LiveDeps, Nothing, Has[Service]] =
    ZLayer.fromServices[Logger.Service, GitHubClient.Service, Service] { (logger: Logger.Service, gitHubClient: GitHubClient.Service) =>
      GitHub(logger, gitHubClient)
    }

  def validate(text: String): ZIO[RepositoryValidator, PagerError, Name] =
    ZIO.accessM[RepositoryValidator](_.get.validate(text))
}
