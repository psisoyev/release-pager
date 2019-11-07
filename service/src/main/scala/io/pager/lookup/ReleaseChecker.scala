package io.pager.lookup

import io.pager.Subscription.RepositoryName
import io.pager.api.github.GitHubClient
import io.pager.api.http.HttpClient
import io.pager.storage.SubscriptionRepository
import zio.clock._

trait ReleaseChecker {
  val releaseChecker: ReleaseChecker.Service
}

object ReleaseChecker {
  trait Service {
    def scheduleRefresh(name: RepositoryName): Unit
  }

  trait Live extends ReleaseChecker with Clock {
    val repository: SubscriptionRepository.Service
    val gitHubClient: GitHubClient.Service

    override val releaseChecker: Service = new Service {
      override def scheduleRefresh(name: RepositoryName): Unit =
        repository.listRepositories.map { repositories =>
          repositories.map {
            case (name, status) =>
              gitHubClient.releases(name).map { releases =>
              }

          }

        }
    }
  }
}
