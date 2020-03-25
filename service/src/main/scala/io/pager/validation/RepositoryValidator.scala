package io.pager.validation

import io.pager.PagerError
import io.pager.client.github.GitHubClient
import io.pager.client.github.GitHubClient.GitHubClient
import io.pager.log.Logger
import io.pager.log.Logger.Logger
import io.pager.subscription.Repository.Name
import zio.macros.accessible
import zio.{ Has, IO, ZIO, ZLayer }

@accessible
object RepositoryValidator {
  type RepositoryValidator = Has[Service]

  trait Service {
    def validate(text: String): IO[PagerError, Name]
  }

  type LiveDeps = Logger with GitHubClient
  def live: ZLayer[LiveDeps, Nothing, Has[Service]] =
    ZLayer.fromServices[Logger.Service, GitHubClient.Service, Service] { (logger, gitHubClient) =>
      GitHub(logger, gitHubClient)
    }
}
