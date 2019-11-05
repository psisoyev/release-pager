package io.pager.api.github

import io.pager.PagerError
import io.pager.PagerError.NotFound
import io.pager.Subscription.RepositoryUrl
import io.pager.api.http.HttpClient
import io.pager.logging.Logger
import zio.{ IO, ZIO }

trait GitHubClient {
  val gitHubClient: GitHubClient.Service
}

object GitHubClient {
  trait Service {
    def repositoryExists(url: RepositoryUrl): IO[PagerError, RepositoryUrl]
  }

  trait Live extends GitHubClient {
    val logger: Logger.Service
    val httpClient: HttpClient.Service

    override val gitHubClient: Service = new Service {
      override def repositoryExists(url: RepositoryUrl): IO[PagerError, RepositoryUrl] =
        httpClient
          .get(s"$url/releases")
          .foldM(
            _ => ZIO.fail(NotFound(url.value)),
            _ => ZIO.succeed(url)
          )
    }
  }
}
