package io.pager.validation

import io.pager.PagerError.NotFound
import io.pager.api.http.HttpClient
import io.pager.logging.Logger
import io.pager.{ PagerError, Subscription }
import zio.ZIO

trait GitHubRepositoryValidator extends RepositoryValidator {
  val logger: Logger.Service
  val httpClient: HttpClient.Service

  def validator: RepositoryValidator.Service = new RepositoryValidator.Service {
    def validate(name: String): ZIO[Any, PagerError, Subscription.RepositoryUrl] = {
      def failure: ZIO[Any, PagerError, Nothing] =
        logger.info(s"Failed to find repository $name") *> ZIO.fail(NotFound(name))

      def success(url: String): ZIO[Any, PagerError, Subscription.RepositoryUrl] =
        logger.info(s"Validated repository $name") *> ZIO.succeed(Subscription.RepositoryUrl(url))

      //      TODO remove last /
      val url =
        if (name.startsWith("https://github.com/") || name.startsWith("http://github.com/")) name
        else s"https://github.com/$name"

      httpClient
        .get(s"$url/releases")
        .foldM(
          _ => failure,
          _ => success(url)
        )
    }
  }
}
