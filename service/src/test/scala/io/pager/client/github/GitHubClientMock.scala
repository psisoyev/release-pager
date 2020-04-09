package io.pager.client.github

import io.pager.PagerError
import io.pager.client.github.GitHubClient.GitHubClient
import io.pager.subscription.Repository.Name
import zio._
import zio.test.mock._

//@mockable[GitHubClient.Service]
//object GitHubClientMock

// Using `mockable` we generate code below
object GitHubClientMock extends Mock[GitHubClient] {
  object RepositoryExists extends Effect[Name, PagerError, Name]
  object Releases         extends Effect[Name, PagerError, List[GitHubRelease]]

  val compose: URLayer[Has[Proxy], GitHubClient] =
    ZLayer.fromService { proxy =>
      new GitHubClient.Service {
        override def repositoryExists(name: Name): IO[PagerError, Name]        = proxy(GitHubClientMock.RepositoryExists, name)
        override def releases(name: Name): IO[PagerError, List[GitHubRelease]] = proxy(GitHubClientMock.Releases, name)
      }
    }
}
