package io.pager.client.github

import io.pager.PagerError
import io.pager.client.github.GitHubClient.GitHubClient
import io.pager.subscription.Repository.Name
import zio.test.mock._
import zio.{ Has, IO, URLayer, ZLayer }

object GitHubClientMock extends Mock[GitHubClient] {
  object repositoryExists extends Effect[Name, PagerError, Name]
  object releases         extends Effect[Name, PagerError, List[GitHubRelease]]

  val compose: URLayer[Has[Proxy], GitHubClient] =
    ZLayer.fromService { proxy =>
      new GitHubClient.Service {
        override def repositoryExists(name: Name): IO[PagerError, Name]        = proxy(GitHubClientMock.repositoryExists, name)
        override def releases(name: Name): IO[PagerError, List[GitHubRelease]] = proxy(GitHubClientMock.releases, name)
      }
    }
}
