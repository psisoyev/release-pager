package io.pager.client.github

import io.pager.PagerError
import io.pager.client.github.GitHubClient.GitHubClient
import io.pager.subscription.Repository.Name
import zio.test.mock.Proxy
import zio.test.mock.Method
import zio.{ Has, IO, URLayer, ZLayer }

object GitHubClientMock {
  object repositoryExists extends Tag[Name, Name]
  object releases         extends Tag[Name, List[GitHubRelease]]

  sealed class Tag[I, A] extends Method[GitHubClient, I, A] {
    override def envBuilder: URLayer[Has[Proxy], GitHubClient] = ZLayer.fromService { invoke =>
      new GitHubClient.Service {
        override def repositoryExists(name: Name): IO[PagerError, Name]        = invoke(GitHubClientMock.repositoryExists, name)
        override def releases(name: Name): IO[PagerError, List[GitHubRelease]] = invoke(GitHubClientMock.releases, name)
      }
    }
  }
}
