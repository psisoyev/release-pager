package io.pager.api.github

import io.circe.generic.semiauto.{ deriveDecoder, deriveEncoder }
import io.circe.{ Decoder, Encoder }
import io.pager.PagerError.NotFound
import io.pager.Subscription.RepositoryName
import io.pager.api.http.HttpClient
import io.pager.logging.Logger
import io.pager.{ GitHubRelease, PagerError }
import zio.{ IO, ZIO }

trait GitHubClient {
  val gitHubClient: GitHubClient.Service
}

object GitHubClient {
  trait Service {
    def repositoryExists(name: RepositoryName): IO[PagerError, RepositoryName]
    def releases(name: RepositoryName): IO[PagerError, List[GitHubRelease]]
  }

  trait Live extends GitHubClient {
    val logger: Logger.Service
    val httpClient: HttpClient.Service

    implicit val encoder: Encoder[GitHubRelease] = deriveEncoder
    implicit val decoder: Decoder[GitHubRelease] = deriveDecoder

    override val gitHubClient: Service = new Service {
      override def repositoryExists(name: RepositoryName): IO[PagerError, RepositoryName] =
        releases(name).map(_ => name)

      override def releases(name: RepositoryName): IO[PagerError, List[GitHubRelease]] =
        httpClient
          .get[List[GitHubRelease]](s"https://api.github.com/repos/${name.value}/releases")
          .foldM(_ => ZIO.fail(NotFound(name.value)), ZIO.succeed)
    }
  }
}
