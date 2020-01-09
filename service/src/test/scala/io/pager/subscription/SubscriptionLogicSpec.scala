package io.pager.subscription

import io.pager.Generators._
import io.pager.TestScenarios
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

  private def service: UIO[SubscriptionLogic.Service[Any]] =
    for {
      repositoryMap   <- Ref.make(Map.empty[Name, Option[Version]])
      subscriptionMap <- Ref.make(Map.empty[ChatId, Set[Name]])
    } yield {
      val chatStorage              = ChatStorage.Test.make(subscriptionMap)
      val repositoryVersionStorage = RepositoryVersionStorage.Test.make(repositoryMap)

      SubscriptionLogic.Live.make(
        logger = Logger.Test,
        chatStorageService = chatStorage,
        repositoryVersionStorageService = repositoryVersionStorage
      )
    }

  val scenarios: TestScenarios = List(
    testM("successfully subscribe to a repository") {
      checkM(repositoryName, chatId) { (name, chatId) =>
        for {
          service        <- service
          _              <- service.subscribe(chatId, name)
          repositories   <- service.listRepositories
          subscriptions1 <- service.listSubscriptions(chatId)
          subscribers    <- service.listSubscribers(name)
        } yield {
          assert(repositories, equalTo(Set(name))) &&
          assert(subscriptions1, equalTo(Set(name))) &&
          assert(subscribers, equalTo(Set(chatId)))
        }
      }
    },
    testM("successfully subscribe to a repository") {
      checkM(repositoryName, chatIds) {
        case (name, (chatId1, chatId2)) =>
          for {
            service        <- service
            _              <- service.subscribe(chatId1, name)
            repositories   <- service.listRepositories
            subscriptions1 <- service.listSubscriptions(chatId1)
            subscriptions2 <- service.listSubscriptions(chatId2)
            subscribers    <- service.listSubscribers(name)
          } yield {
            assert(repositories, equalTo(Set(name))) &&
            assert(subscriptions1, equalTo(Set(name))) &&
            assert(subscriptions2, equalTo(Set())) &&
            assert(subscribers, equalTo(Set(chatId1)))
          }
      }
    },
    testM("successfully subscribe to a repository twice") {
      checkM(repositoryName, chatIds) {
        case (name, (chatId1, chatId2)) =>
          for {
            service        <- service
            _              <- service.subscribe(chatId1, name)
            _              <- service.subscribe(chatId1, name)
            repositories   <- service.listRepositories
            subscriptions1 <- service.listSubscriptions(chatId1)
            subscriptions2 <- service.listSubscriptions(chatId2)
            subscribers    <- service.listSubscribers(name)
          } yield {
            assert(repositories, equalTo(Set(name))) &&
            assert(subscriptions1, equalTo(Set(name))) &&
            assert(subscriptions2, equalTo(Set())) &&
            assert(subscribers, equalTo(Set(chatId1)))
          }
      }
    },
    testM("successfully subscribe to a repository from two chats") {
      checkM(repositoryName, chatIds) {
        case (name, (chatId1, chatId2)) =>
          for {
            service        <- service
            _              <- service.subscribe(chatId1, name)
            _              <- service.subscribe(chatId2, name)
            repositories   <- service.listRepositories
            subscriptions1 <- service.listSubscriptions(chatId1)
            subscriptions2 <- service.listSubscriptions(chatId2)
            subscribers    <- service.listSubscribers(name)
          } yield {
            assert(repositories, equalTo(Set(name))) &&
            assert(subscriptions1, equalTo(Set(name))) &&
            assert(subscriptions2, equalTo(Set(name))) &&
            assert(subscribers, equalTo(Set(chatId1, chatId2)))
          }
      }
    },
    testM("allow to unsubscribe from non-subscribed repository") {
      checkM(repositoryName, chatId) { (name, chatId1) =>
        for {
          service       <- service
          _             <- service.unsubscribe(chatId1, name)
          repositories  <- service.listRepositories
          subscriptions <- service.listSubscriptions(chatId1)
          subscribers   <- service.listSubscribers(name)
        } yield {
          assert(repositories, equalTo(Set())) &&
          assert(subscriptions, equalTo(Set())) &&
          assert(subscribers, equalTo(Set()))

        }
      }
    },
    testM("allow to unsubscribe from subscribed repository") {
      checkM(repositoryName, chatId) { (name, chatId1) =>
        for {
          service       <- service
          _             <- service.subscribe(chatId1, name)
          _             <- service.unsubscribe(chatId1, name)
          repositories  <- service.listRepositories
          subscriptions <- service.listSubscriptions(chatId1)
          subscribers   <- service.listSubscribers(name)
        } yield {
          assert(repositories, equalTo(Set())) &&
          assert(subscriptions, equalTo(Set())) &&
          assert(subscribers, equalTo(Set()))
        }
      }
    }
  )
}
