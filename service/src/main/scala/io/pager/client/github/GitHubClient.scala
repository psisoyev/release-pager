package io.pager.client.github

import io.circe.generic.semiauto.{ deriveDecoder, deriveEncoder }
import io.circe.{ Decoder, Encoder }
import io.pager.PagerError
import io.pager.PagerError.NotFound
import io.pager.client.http.HttpClient
import io.pager.logging.Logger
import io.pager.subscription.RepositoryName
import io.pager.subscription.RepositoryStatus.Version
import zio.{ IO, ZIO }

trait GitHubClient {
  val gitHubClient: GitHubClient.Service
}

object GitHubClient {
  trait Service {
    def repositoryExists(name: RepositoryName): IO[PagerError, RepositoryName]
    def releases(name: RepositoryName): IO[PagerError, List[GitHubRelease]]
  }

  implicit val versionEncoder: Encoder[Version]             = deriveEncoder
  implicit val versionDecoder: Decoder[Version]             = deriveDecoder
  implicit val gitHubReleaseEncoder: Encoder[GitHubRelease] = deriveEncoder
  implicit val gitHubReleaseDecoder: Decoder[GitHubRelease] = deriveDecoder

  trait Live extends GitHubClient {
    val logger: Logger.Service
    val httpClient: HttpClient.Service

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
