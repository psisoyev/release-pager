package io.pager.client.github

import io.circe.Decoder
import io.circe.generic.extras.semiauto.deriveUnwrappedDecoder
import io.circe.generic.semiauto.deriveDecoder
import io.pager.PagerError
import io.pager.PagerError.NotFound
import io.pager.client.http.HttpClient
import io.pager.logging.Logger
import io.pager.subscription.Repository.{ Name, Version }
import zio.{ Has, IO, ZIO, ZLayer }

object GitHubClient {
  type GitHubClient = Has[Service]

  trait Service {
    def repositoryExists(name: Name): IO[PagerError, Name]
    def releases(name: Name): IO[PagerError, List[GitHubRelease]]
  }

  implicit val versionDecoder: Decoder[Version]             = deriveUnwrappedDecoder
  implicit val gitHubReleaseDecoder: Decoder[GitHubRelease] = deriveDecoder

  final case class Live(logger: Logger.Service, httpClient: HttpClient.Service) extends Service {
    override def repositoryExists(name: Name): IO[PagerError, Name] =
      releases(name).as(name)

    override def releases(name: Name): IO[PagerError, List[GitHubRelease]] = {
      val url = s"https://api.github.com/repos/${name.value}/releases"
      logger.info(s"Checking releases of ${name.value}: $url") *>
        httpClient
          .get[List[GitHubRelease]](url)
          .foldM(
            e => logger.warn(s"Couldn't find repository ${name.value}: ${e.message}") *> ZIO.fail(NotFound(name.value)),
            releases => ZIO.succeed(releases)
          )
    }
  }

  val live: ZLayer[Any, Nothing, Has[Live]] = ZLayer.fromFunction {
    case (logger: Logger.Service, httpClient: HttpClient.Service) =>
      Live(logger, httpClient)
  }
}
