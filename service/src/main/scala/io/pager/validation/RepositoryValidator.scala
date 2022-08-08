package io.pager.validation

import io.pager.PagerError
import io.pager.client.github.GitHubClient
import io.pager.log.Logger
import io.pager.subscription.Repository.Name
import zio.{ IO, URLayer, ZIO, ZLayer }

trait RepositoryValidator {
  def validate(text: String): IO[PagerError, Name]
}

object RepositoryValidator {
  type LiveDeps = Logger with GitHubClient
  val live: URLayer[LiveDeps, RepositoryValidator] = ZLayer {
    for {
      logger <- ZIO.service[Logger]
      client <- ZIO.service[GitHubClient]
    } yield GitHub(logger, client)
  }
}
