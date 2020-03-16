package io.pager.client.github

import io.pager.PagerError
import io.pager.client.http.HttpClient
import io.pager.client.http.HttpClient.HttpClient
import io.pager.log.Logger
import io.pager.log.Logger.Logger
import io.pager.subscription.Repository.Name
import zio.{ Has, IO, Layer, ULayer, URLayer, ZIO, ZLayer }

object GitHubClient {
  type GitHubClient = Has[Service]

  trait Service {
    def repositoryExists(name: Name): IO[PagerError, Name]
    def releases(name: Name): IO[PagerError, List[GitHubRelease]]
  }

  def live: URLayer[Logger with HttpClient, Has[Service]] =
    ZLayer.fromServices[Logger.Service, HttpClient.Service, Service] { (logger, httpClient) =>
      Live(logger, httpClient)
    }

  def empty: ULayer[Has[Service]] =
    ZLayer.succeed(new Service {
      override def repositoryExists(name: Name): IO[PagerError, Name]        = ???
      override def releases(name: Name): IO[PagerError, List[GitHubRelease]] = ???
    })
}
