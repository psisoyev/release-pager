package io.pager.validation

import io.pager.PagerError.NotFound
import io.pager.api.http._
import io.pager.logger._
import io.pager.{LoggingEnv, PagerError, Subscription, ValidatorEnv}
import zio.ZIO

trait GitHubRepositoryValidator extends RepositoryValidator.Service[ValidatorEnv] {
  def validate(name: String): ZIO[ValidatorEnv, PagerError, Subscription.RepositoryUrl] = {
    def failure: ZIO[LoggingEnv, PagerError, Nothing] =
      info(s"Failed to find repository $name") *> ZIO.fail(NotFound(name))

    def success(url: String): ZIO[LoggingEnv, PagerError, Subscription.RepositoryUrl] =
      info(s"Validated repository $name") *> ZIO.succeed(Subscription.RepositoryUrl(url))

    //      TODO remove last /
    val url =
      if (name.startsWith("https://github.com/") || name.startsWith("http://github.com/")) name
      else s"https://github.com/$name"

    get(s"$url/releases")
      .foldM(
        _ => failure,
        _ => success(url)
      )
  }
}
