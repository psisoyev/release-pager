package io.pager
package subscription

import io.pager.Generators._
import io.pager.TestData._
import io.pager.client.telegram.ChatId
import io.pager.log.Logger
import io.pager.subscription.Repository.{ Name, Version }
import io.pager.subscription.chat.ChatStorage
import io.pager.subscription.repository.RepositoryVersionStorage
import zio._
import zio.test.Assertion._
import zio.test._

object LiveSubscriptionLogicSpec extends DefaultRunnableSpec {

  type RepositoryMap   = ULayer[Has[Ref[Map[Name, Option[Version]]]]]
  type SubscriptionMap = ULayer[Has[Ref[Map[ChatId, Set[Name]]]]]

  private def service(
    subscriptionMap: Map[ChatId, Set[Name]] = Map.empty,
    repositoryMap: Map[Name, Option[Version]] = Map.empty
  ): ULayer[Has[SubscriptionLogic.Service]] = {
    val chatStorage              = Ref.make(subscriptionMap).toLayer >>> ChatStorage.inMemory
    val repositoryVersionStorage = Ref.make(repositoryMap).toLayer >>> RepositoryVersionStorage.inMemory

    (Logger.silent ++ chatStorage ++ repositoryVersionStorage) >>> SubscriptionLogic.live
  }

  override def spec: ZSpec[Environment, Failure] = suite("LiveSubscriptionLogicSpec")(
    testM("return empty subscriptions") {
      checkM(chatId) { chatId =>
        val result = SubscriptionLogic.listSubscriptions(chatId).provideLayer(service())
        assertM(result)(isEmpty)
      }
    },
    testM("return empty subscribers") {
      checkM(repositoryName) { name =>
        val result = SubscriptionLogic.listSubscribers(name).provideLayer(service())
        assertM(result)(isEmpty)
      }
    },
    testM("successfully subscribe to a repository") {
      checkM(repositoryName, chatId) {
        case (name, chatId) =>
          val result = for {
            _             <- SubscriptionLogic.subscribe(chatId, name)
            repositories  <- SubscriptionLogic.listRepositories
            subscriptions <- SubscriptionLogic.listSubscriptions(chatId)
            subscribers   <- SubscriptionLogic.listSubscribers(name)
          } yield {
            assert(repositories)(equalTo(Map(name -> None))) &&
            assert(subscriptions)(equalTo(Set(name))) &&
            assert(subscribers)(equalTo(Set(chatId)))
          }

          result.provideLayer(service())
      }
    },
    testM("successfully subscribe to a repository twice") {
      checkM(repositoryName, chatId) {
        case (name, chatId) =>
          val result = for {
            _             <- SubscriptionLogic.subscribe(chatId, name)
            repositories  <- SubscriptionLogic.listRepositories
            subscriptions <- SubscriptionLogic.listSubscriptions(chatId)
            subscribers   <- SubscriptionLogic.listSubscribers(name)
          } yield {
            assert(repositories)(equalTo(Map(name -> None))) &&
            assert(subscriptions)(equalTo(Set(name))) &&
            assert(subscribers)(equalTo(Set(chatId)))
          }

          result.provideLayer(service(Map(chatId -> Set(name))))
      }
    },
    testM("successfully subscribe to a repository from two chats") {
      checkM(repositoryName, chatIds) {
        case (name, (chatId1, chatId2)) =>
          val result = for {
            _              <- SubscriptionLogic.subscribe(chatId1, name)
            _              <- SubscriptionLogic.subscribe(chatId2, name)
            repositories   <- SubscriptionLogic.listRepositories
            subscriptions1 <- SubscriptionLogic.listSubscriptions(chatId1)
            subscriptions2 <- SubscriptionLogic.listSubscriptions(chatId2)
            subscribers    <- SubscriptionLogic.listSubscribers(name)
          } yield {
            assert(repositories)(equalTo(Map(name -> None))) &&
            assert(subscriptions1)(equalTo(Set(name))) &&
            assert(subscriptions2)(equalTo(Set(name))) &&
            assert(subscribers)(equalTo(Set(chatId1, chatId2)))
          }

          result.provideLayer(service())
      }
    },
    testM("successfully unsubscribe from non-subscribed repository") {
      checkM(repositoryName, chatId) { (name, chatId) =>
        val result = for {
          _             <- SubscriptionLogic.unsubscribe(chatId, name)
          repositories  <- SubscriptionLogic.listRepositories
          subscriptions <- SubscriptionLogic.listSubscriptions(chatId)
          subscribers   <- SubscriptionLogic.listSubscribers(name)
        } yield {
          assert(repositories)(isEmpty) &&
          assert(subscriptions)(isEmpty) &&
          assert(subscribers)(isEmpty)
        }

        result.provideLayer(service())
      }
    },
    testM("successfully unsubscribe from subscribed repository") {
      checkM(repositoryName, chatId) { (name, chatId) =>
        val result = for {
          _             <- SubscriptionLogic.unsubscribe(chatId, name)
          repositories  <- SubscriptionLogic.listRepositories
          subscriptions <- SubscriptionLogic.listSubscriptions(chatId)
          subscribers   <- SubscriptionLogic.listSubscribers(name)
        } yield {
          assert(repositories)(isEmpty) &&
          assert(subscriptions)(isEmpty) &&
          assert(subscribers)(isEmpty)
        }

        result.provideLayer(service(Map(chatId -> Set(name))))
      }
    },
    testM("update repository version") {
      checkM(repositoryName) { name =>
        val result = for {
          _             <- SubscriptionLogic.updateVersions(Map(name -> rcVersion))
          repositories1 <- SubscriptionLogic.listRepositories
          _             <- SubscriptionLogic.updateVersions(Map(name -> finalVersion))
          repositories2 <- SubscriptionLogic.listRepositories
        } yield {
          assert(repositories1)(equalTo(Map(name -> Some(rcVersion)))) &&
          assert(repositories2)(equalTo(Map(name -> Some(finalVersion))))
        }

        result.provideLayer(service(repositoryMap = Map(name -> None)))
      }
    }
  )
}
