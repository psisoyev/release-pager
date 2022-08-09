package io.pager.client.github

import io.circe.Decoder
import io.circe.generic.extras.semiauto.deriveUnwrappedDecoder
import io.circe.generic.semiauto.deriveDecoder
import io.pager.PagerError
import io.pager.PagerError.NotFound
import io.pager.client.github.GitHubClient._
import io.pager.client.http.HttpClient
import io.pager.log.Logger
import io.pager.subscription.Repository.{ Name, Version }
import zio._

trait GitHubClient {
  def repositoryExists(name: Name): IO[PagerError, Name]

  def releases(name: Name): IO[PagerError, List[GitHubRelease]]
}

private[github] final case class Live(logger: Logger, httpClient: HttpClient) extends GitHubClient {
  override def repositoryExists(name: Name): IO[PagerError, Name] =
    releases(name).as(name)

  override def releases(name: Name): IO[PagerError, List[GitHubRelease]] = {
    val url = s"https://api.github.com/repos/${name.value}/releases"

    logger.info(s"Checking releases of ${name.value}: $url") *>
      httpClient
        .get[List[GitHubRelease]](url)
        .foldZIO(
          e => logger.warn(s"Couldn't find repository ${name.value}: ${e.message}") *> ZIO.fail(NotFound(name.value)),
          releases => ZIO.succeed(releases)
        )
  }
}

object GitHubClient {
  implicit val versionDecoder: Decoder[Version]             = deriveUnwrappedDecoder
  implicit val gitHubReleaseDecoder: Decoder[GitHubRelease] = deriveDecoder

  val live: URLayer[Logger with HttpClient, GitHubClient] = ZLayer {
    for {
      logger <- ZIO.service[Logger]
      client <- ZIO.service[HttpClient]
    } yield Live(logger, client)
  }

  def empty: ULayer[GitHubClient] =
    ZLayer.succeed(new GitHubClient {
      override def repositoryExists(name: Name): IO[PagerError, Name]        = ???
      override def releases(name: Name): IO[PagerError, List[GitHubRelease]] = ???
    })
}
