package io.pager.subscription

import io.pager.Generators
import io.pager.TestData.{ chatId1, chatId2 }
import io.pager.client.telegram.ChatId
import io.pager.logging.Logger
import io.pager.subscription.Repository.{ Name, Version }
import io.pager.subscription.SubscriptionLogicTestCases._
import zio._
import zio.test.Assertion._
import zio.test._

object SubscriptionLogicSpec extends DefaultRunnableSpec(suite(specName)(scenarios: _*))

object SubscriptionLogicTestCases {
  val specName: String = "SubscriptionLogicSpec"

  private def repo: UIO[SubscriptionLogic.Service[Any]] =
    for {
      repositoryMap   <- Ref.make(Map.empty[Name, Option[Version]])
      subscriptionMap <- Ref.make(Map.empty[ChatId, Set[Name]])
    } yield {
      val testChatStorage              = ChatStorage.Test.make(subscriptionMap).chatStorage
      val testRepositoryVersionStorage = RepositoryVersionStorage.Test.make(repositoryMap).repositoryVersionStorage

      new SubscriptionLogic.Live {
        override def logger: Logger.Service                                     = Logger.Test
        override def chatStorage: ChatStorage.Service                           = testChatStorage
        override def repositoryVersionStorage: RepositoryVersionStorage.Service = testRepositoryVersionStorage
      }.subscriptionLogic
    }

  val scenarios = Seq(
    testM("successfully subscribe to a repository") {
      checkM(Generators.repositoryName) { name =>
        for {
          repo           <- repo
          _              <- repo.subscribe(chatId1, name)
          repositories   <- repo.listRepositories
          subscriptions1 <- repo.listSubscriptions(chatId1)
          subscriptions2 <- repo.listSubscriptions(chatId2)
          subscribers    <- repo.listSubscribers(name)
        } yield {
          assert(repositories, equalTo(Set(name)))
          assert(subscriptions1, equalTo(Set(name)))
          assert(subscriptions2, equalTo(Set()))
          assert(subscribers, equalTo(Set(chatId1)))
        }
      }
    },
    testM("successfully subscribe to a repository twice") {
      checkM(Generators.repositoryName) { name =>
        for {
          repo           <- repo
          _              <- repo.subscribe(chatId1, name)
          _              <- repo.subscribe(chatId1, name)
          repositories   <- repo.listRepositories
          subscriptions1 <- repo.listSubscriptions(chatId1)
          subscriptions2 <- repo.listSubscriptions(chatId2)
          subscribers    <- repo.listSubscribers(name)
        } yield {
          assert(repositories, equalTo(Set(name)))
          assert(subscriptions1, equalTo(Set(name)))
          assert(subscriptions2, equalTo(Set()))
          assert(subscribers, equalTo(Set(chatId1)))
        }
      }
    },
    testM("successfully subscribe to a repository from two chats") {
      checkM(Generators.repositoryName) { name =>
        for {
          repo           <- repo
          _              <- repo.subscribe(chatId1, name)
          _              <- repo.subscribe(chatId2, name)
          repositories   <- repo.listRepositories
          subscriptions1 <- repo.listSubscriptions(chatId1)
          subscriptions2 <- repo.listSubscriptions(chatId2)
          subscribers    <- repo.listSubscribers(name)
        } yield {
          assert(repositories, equalTo(Set(name)))
          assert(subscriptions1, equalTo(Set(name)))
          assert(subscriptions2, equalTo(Set(name)))
          assert(subscribers, equalTo(Set(chatId1, chatId2)))
        }
      }
    },
    testM("allow to unsubscribe from non-subscribed repository") {
      checkM(Generators.repositoryName) { name =>
        for {
          repo          <- repo
          _             <- repo.unsubscribe(chatId1, name)
          repositories  <- repo.listRepositories
          subscriptions <- repo.listSubscriptions(chatId1)
          subscribers   <- repo.listSubscribers(name)
        } yield {
          assert(repositories, equalTo(Set()))
          assert(subscriptions, equalTo(Set()))
          assert(subscribers, equalTo(Set()))

        }
      }
    },
    testM("allow to unsubscribe from subscribed repository") {
      checkM(Generators.repositoryName) { name =>
        for {
          repo          <- repo
          _             <- repo.subscribe(chatId1, name)
          _             <- repo.unsubscribe(chatId1, name)
          repositories  <- repo.listRepositories
          subscriptions <- repo.listSubscriptions(chatId1)
          subscribers   <- repo.listSubscribers(name)
        } yield {
          assert(repositories, equalTo(Set()))
          assert(subscriptions, equalTo(Set()))
          assert(subscribers, equalTo(Set()))
        }
      }
    }
  )
}
