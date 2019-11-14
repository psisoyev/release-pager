package io.pager.api.telegram

import canoe.api.models.ChatApi
import canoe.api.{ TelegramClient => Client, _ }
import canoe.models.messages.TextMessage
import canoe.models.outgoing.TextContent
import canoe.models.{ Chat, PrivateChat }
import canoe.syntax._
import io.pager.Subscription.ChatId
import io.pager.logging.Logger
import io.pager.subscription._
import io.pager.validation._
import io.pager.{ RepositoryStatus, Subscription }
import zio._
import zio.interop.catz._

trait TelegramClient {
  val telegramClient: TelegramClient.Service
}

object TelegramClient {
  trait Service {
    def start: Task[Unit]
    def broadcastNewVersion(repositoryStatus: RepositoryStatus): Task[Unit]
  }

  trait Canoe extends TelegramClient {
    def logger: Logger.Service
    implicit def canoeClient: Client[Task]
    def repositoryValidator: RepositoryValidator.Service
    def subscriptionRepository: SubscriptionRepository.Service

    override val telegramClient: Service = new Service {
      def broadcastNewVersion(repositoryStatus: RepositoryStatus): Task[Unit] =
        repositoryStatus.version.map { version =>
          ZIO
            .traverse(repositoryStatus.subscribers) { chatId =>
              val api = new ChatApi(PrivateChat(chatId.value, None, None, None))
              api.send(TextContent(s"HELLO PRIVET $version"))
            }
            .unit
        }.getOrElse(UIO.unit)

      def addRepository: Scenario[Task, Unit] =
        for {
          chat      <- Scenario.start(command("add").chat)
          _         <- Scenario.eval(chat.send("Please provide repository in form 'organization/name'"))
          _         <- Scenario.eval(chat.send("Examples: psisoyev/release-pager or zio/zio"))
          userInput <- Scenario.next(text)
          _         <- Scenario.eval(chat.send(s"Checking repository $userInput"))
          _ <- Scenario.eval(
                repositoryValidator
                  .validate(userInput)
                  .foldM(
                    e => chat.send(s"Couldn't add repository $userInput: ${e.message}"),
                    name => chat.send(s"Added repository $userInput") *> subscriptionRepository.subscribe(Subscription(ChatId(chat.id), name))
                  )
              )
        } yield ()

      def listRepositories: Scenario[Task, Unit] =
        for {
          chat  <- Scenario.start(command("list").chat)
          repos <- Scenario.eval(subscriptionRepository.listSubscriptions(ChatId(chat.id)))
          _ <- {
            val result =
              if (repos.isEmpty) chat.send("You don't have subscriptions yet")
              else chat.send("Listing your subscriptions:") *> ZIO.foreach(repos)(name => chat.send(name.value))

            Scenario.eval(result)
          }
        } yield ()

      def help: Scenario[Task, Unit] =
        for {
          chat <- Scenario.start(command("help").chat)
          _    <- broadcastHelp(chat)
        } yield ()

      def startBot: Scenario[Task, Unit] =
        for {
          chat <- Scenario.start(command("start").chat)
          _    <- broadcastHelp(chat)
        } yield ()

      private def broadcastHelp(chat: Chat): Scenario[Task, TextMessage] = {
        val helpText =
          """
            |/help Shows this menu
            |/add Subscribe to GitHub project releases
            |/del Delete subscription
            |/list List current subscriptions
            |""".stripMargin

        Scenario.eval(chat.send(helpText))
      }

      override def start: Task[Unit] =
        logger.info("starting telegram polling") *>
          Bot
            .polling[Task]
            .follow(startBot, help, addRepository, listRepositories)
            .compile
            .drain
    }
  }
}
