package io.pager
package subscription

import io.pager.Generators._
import io.pager.client.telegram.ChatId
import io.pager.logging.Logger
import io.pager.subscription.Repository.{ Name, Version }
import zio._
import zio.test.Assertion._
import zio.test._
import TestData._
import io.pager.TestCases.TestScenarios

object LiveSubscriptionLogicSpec extends DefaultRunnableSpec(LiveSubscriptionLogicTestCases.suite)

object LiveSubscriptionLogicTestCases extends TestCases {
  val specName: String = "LiveSubscriptionLogicSpec"
  type RepositoryMap   = UIO[Ref[Map[Name, Option[Version]]]]
  type SubscriptionMap = UIO[Ref[Map[ChatId, Set[Repository.Name]]]]

  private def service(
    subscriptionMap: SubscriptionMap = emptyMap[ChatId, Set[Name]],
    repositoryMap: RepositoryMap = emptyMap[Name, Option[Version]]
  ): UIO[SubscriptionLogic.Service[Any]] =
    for {
      repositoryMap   <- repositoryMap
      subscriptionMap <- subscriptionMap
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
    testM("return empty subscriptions") {
      checkM(repositoryName, chatId) {
        case (name, chatId) =>
          for {
            service       <- service()
            subscriptions <- service.listSubscriptions(chatId)
            subscribers   <- service.listSubscribers(name)
          } yield {
            assert(subscriptions, isEmpty) &&
            assert(subscribers, isEmpty)
          }
      }
    },
    testM("successfully subscribe to a repository") {
      checkM(repositoryName, chatId) {
        case (name, chatId) =>
          for {
            service       <- service()
            _             <- service.subscribe(chatId, name)
            repositories  <- service.listRepositories
            subscriptions <- service.listSubscriptions(chatId)
            subscribers   <- service.listSubscribers(name)
          } yield {
            assert(repositories, equalTo(Map(name -> None))) &&
            assert(subscriptions, equalTo(Set(name))) &&
            assert(subscribers, equalTo(Set(chatId)))
          }
      }
    },
    testM("successfully subscribe to a repository twice") {
      checkM(repositoryName, chatId) {
        case (name, chatId) =>
          for {
            service       <- service(mkMap(Map(chatId -> Set(name))))
            _             <- service.subscribe(chatId, name)
            repositories  <- service.listRepositories
            subscriptions <- service.listSubscriptions(chatId)
            subscribers   <- service.listSubscribers(name)
          } yield {
            assert(repositories, equalTo(Map(name -> None))) &&
            assert(subscriptions, equalTo(Set(name))) &&
            assert(subscribers, equalTo(Set(chatId)))
          }
      }
    },
    testM("successfully subscribe to a repository from two chats") {
      checkM(repositoryName, chatIds) {
        case (name, (chatId1, chatId2)) =>
          for {
            service        <- service()
            _              <- service.subscribe(chatId1, name)
            _              <- service.subscribe(chatId2, name)
            repositories   <- service.listRepositories
            subscriptions1 <- service.listSubscriptions(chatId1)
            subscriptions2 <- service.listSubscriptions(chatId2)
            subscribers    <- service.listSubscribers(name)
          } yield {
            assert(repositories, equalTo(Map(name -> None))) &&
            assert(subscriptions1, equalTo(Set(name))) &&
            assert(subscriptions2, equalTo(Set(name))) &&
            assert(subscribers, equalTo(Set(chatId1, chatId2)))
          }
      }
    },
    testM("successfully unsubscribe from non-subscribed repository") {
      checkM(repositoryName, chatId) { (name, chatId) =>
        for {
          service       <- service()
          _             <- service.unsubscribe(chatId, name)
          repositories  <- service.listRepositories
          subscriptions <- service.listSubscriptions(chatId)
          subscribers   <- service.listSubscribers(name)
        } yield {
          assert(repositories, isEmpty) &&
          assert(subscriptions, isEmpty) &&
          assert(subscribers, isEmpty)
        }
      }
    },
    testM("successfully unsubscribe from subscribed repository") {
      checkM(repositoryName, chatId) { (name, chatId) =>
        for {
          service       <- service(mkMap(Map(chatId -> Set(name))))
          _             <- service.unsubscribe(chatId, name)
          repositories  <- service.listRepositories
          subscriptions <- service.listSubscriptions(chatId)
          subscribers   <- service.listSubscribers(name)
        } yield {
          assert(repositories, isEmpty) &&
          assert(subscriptions, isEmpty) &&
          assert(subscribers, isEmpty)
        }
      }
    },
    testM("update repository version") {
      checkM(repositoryName) { name =>
        for {
          service       <- service(repositoryMap = mkMap(Map(name -> None)))
          _             <- service.updateVersions(Map(name -> rcVersion))
          repositories1 <- service.listRepositories
          _             <- service.updateVersions(Map(name -> finalVersion))
          repositories2 <- service.listRepositories
        } yield {
          assert(repositories1, equalTo(Map(name -> Some(rcVersion)))) &&
          assert(repositories2, equalTo(Map(name -> Some(finalVersion))))
        }
      }
    }
  )
}
