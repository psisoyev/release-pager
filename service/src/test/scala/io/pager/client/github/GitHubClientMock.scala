package io.pager.client.github

//import zio.test.mock.mockable
//
//@mockable[GitHubClient]
//object GitHubClientMock

// Using `mockable` we generate code below
//object GitHubClientMock extends Mock[GitHubClient] {
//  object RepositoryExists extends Effect[Name, PagerError, Name]
//  object Releases         extends Effect[Name, PagerError, List[GitHubRelease]]
//
//  val compose: URLayer[Has[Proxy], GitHubClient] =
//    ZLayer.fromService { proxy =>
//      new GitHubClient {
//        override def repositoryExists(name: Name): IO[PagerError, Name]        = proxy(GitHubClientMock.RepositoryExists, name)
//        override def releases(name: Name): IO[PagerError, List[GitHubRelease]] = proxy(GitHubClientMock.Releases, name)
//      }
//    }
//}
