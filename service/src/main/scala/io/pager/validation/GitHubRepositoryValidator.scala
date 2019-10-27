package io.pager.validation

import io.pager.PagerError.NotFound
import io.pager.api.http.HttpClient
import io.pager.logger._
import io.pager.{ AppEnv, PagerError, Subscription }
import zio.ZIO

trait GitHubRepositoryValidator extends RepositoryValidator {
  def httpClient: HttpClient.Service

  override val validator: RepositoryValidator.Service = new RepositoryValidator.Service {
    def validate(name: String): ZIO[AppEnv, PagerError, Subscription.RepositoryUrl] = {
      def failure(e: Throwable): ZIO[Logger, PagerError, Nothing] =
        info(s"Failed to find repository $name") *> ZIO.fail(NotFound(name))

      def success(s: String): ZIO[Logger, PagerError, Subscription.RepositoryUrl] =
        info(s"Validated repository $name") *> ZIO.succeed(Subscription.RepositoryUrl(name))

//      TODO remove last /
      val url =
        if (name.startsWith("https://github.com/") || name.startsWith("http://github.com/")) name
        else s"https://github.com/$name"

      httpClient
        .get(s"$url/releases")
        .foldM(failure, success)
    }
  }
}
