package io.pager.lookup

import io.pager.Subscription.RepositoryUrl
import io.pager.api.http.HttpClient
import io.pager.storage.SubscriptionRepository
import zio.clock._

trait ReleaseChecker {
  val releaseChecker: ReleaseChecker.Service
}

object ReleaseChecker {
  trait Service {
    def scheduleRefresh(url: RepositoryUrl): Unit
  }

  trait Live extends ReleaseChecker with Clock {
    val repository: SubscriptionRepository.Service
    val httpClient: HttpClient.Service

    override val releaseChecker: Service = new Service {
      override def scheduleRefresh(url: RepositoryUrl): Unit = {
        repository.listRepositories.map(_.map { url =>
          httpClient.get(url.value)
        })

        ???
      }
    }
  }
}
