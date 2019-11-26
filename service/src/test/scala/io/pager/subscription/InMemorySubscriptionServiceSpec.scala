package io.pager.subscription

import io.pager.api.telegram.ChatId
import io.pager.subscription.InMemorySubscriptionRepositoryTestCases._
import zio._
import zio.test.Assertion._
import zio.test._

object InMemorySubscriptionRepositorySpec extends DefaultRunnableSpec(suite(specName)(seq: _*))

object InMemorySubscriptionRepositoryTestCases {
  val specName: String = "InMemorySubscriptionRepositorySpec"

  private def repo: UIO[Subscription.Service] =
    for {
      subscriberMap   <- Ref.make(Map.empty[RepositoryName, RepositoryStatus])
      subscriptionMap <- Ref.make(Map.empty[ChatId, Set[RepositoryName]])
    } yield new Subscription.InMemory {
      override def subscribers: Ref[SubscriberMap]     = subscriberMap
      override def subscriptions: Ref[SubscriptionMap] = subscriptionMap
    }.subscription

  private val chatId1 = ChatId(478912)
  private val chatId2 = ChatId(478913)
  private val url     = RepositoryName("https://github.com/zio/zio")

  val seq = Seq(
    testM("successfully subscribe to a repository") {
      for {
        repo           <- repo
        _              <- repo.subscribe(chatId1, url)
        repositories   <- repo.listRepositories
        subscriptions1 <- repo.listSubscriptions(chatId1)
        subscriptions2 <- repo.listSubscriptions(chatId2)
      } yield {
        assert(repositories, equalTo(Set(url)))
        assert(subscriptions1, equalTo(Set(url)))
        assert(subscriptions2, equalTo(Set()))
      }
    },
    testM("successfully subscribe to a repository twice") {
      for {
        repo           <- repo
        _              <- repo.subscribe(chatId1, url)
        _              <- repo.subscribe(chatId1, url)
        repositories   <- repo.listRepositories
        subscriptions1 <- repo.listSubscriptions(chatId1)
        subscriptions2 <- repo.listSubscriptions(chatId2)
      } yield {
        assert(repositories, equalTo(Set(url)))
        assert(subscriptions1, equalTo(Set(url)))
        assert(subscriptions2, equalTo(Set()))
      }
    },
    testM("successfully subscribe to a repository from two chats") {
      for {
        repo           <- repo
        _              <- repo.subscribe(chatId1, url)
        _              <- repo.subscribe(chatId2, url)
        repositories   <- repo.listRepositories
        subscriptions1 <- repo.listSubscriptions(chatId1)
        subscriptions2 <- repo.listSubscriptions(chatId2)
      } yield {
        assert(repositories, equalTo(Set(url)))
        assert(subscriptions1, equalTo(Set(url)))
        assert(subscriptions2, equalTo(Set(url)))
      }
    },
    testM("allow to unsubscribe from non-subscribed repository") {
      for {
        repo          <- repo
        _             <- repo.unsubscribe(chatId1, url)
        repositories  <- repo.listRepositories
        subscriptions <- repo.listSubscriptions(chatId1)
      } yield {
        assert(repositories, equalTo(Set()))
        assert(subscriptions, equalTo(Set()))
      }
    },
    testM("allow to unsubscribe from subscribed repository") {
      for {
        repo          <- repo
        _             <- repo.subscribe(chatId1, url)
        _             <- repo.unsubscribe(chatId1, url)
        repositories  <- repo.listRepositories
        subscriptions <- repo.listSubscriptions(chatId1)
      } yield {
        assert(repositories, equalTo(Set()))
        assert(subscriptions, equalTo(Set()))
      }
    }
  )
}
