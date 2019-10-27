package io.pager.api.telegram

import canoe.api.{ TelegramClient => Client, _ }
import canoe.models.Chat
import canoe.models.messages.TextMessage
import canoe.syntax._
import fs2.Stream
import io.pager.Subscription.ChatId
import io.pager.storage._
import io.pager.validation.RepositoryValidator
import io.pager.{ AppEnv, AppTask, Subscription }
import zio._
import zio.interop.catz._

trait TelegramClient {
  val telegramClient: TelegramClient.Service
}

object TelegramClient {
  trait Service {
    def start(token: String): AppTask[Unit]
  }

  trait Canoe extends TelegramClient {
    def validator: RepositoryValidator.Service

    override val telegramClient: TelegramClient.Service = new TelegramClient.Service {
      def addRepository(implicit c: Client[AppTask]): Scenario[AppTask, Unit] =
        for {
          chat      <- Scenario.start(command("add").chat)
          _         <- Scenario.eval(chat.send("Please provide a link to the repository or it's 'organization/name'"))
          _         <- Scenario.eval(chat.send("Examples: 'https://github.com/zio/zio' or 'zio/zio'"))
          userInput <- Scenario.next(text)
          _         <- Scenario.eval(chat.send(s"Checking repository $userInput"))
          _ <- Scenario.eval(
                validator
                  .validate(userInput)
                  .foldM(
                    e => chat.send(s"Couldn't add repository $userInput: ${e.message}"),
                    url => chat.send(s"Added repository $userInput") *> subscribe(Subscription(ChatId(chat.id), url))
                  )
              )
        } yield ()

      def listRepositories(implicit c: Client[AppTask]): Scenario[AppTask, Unit] =
        for {
          chat  <- Scenario.start(command("list").chat)
          repos <- Scenario.eval(list(ChatId(chat.id)))
          _ <- {
            val result =
              if (repos.isEmpty) chat.send("You don't have subscriptions yet")
              else chat.send("Listing your subscriptions:") *> ZIO.foreach(repos)(url => chat.send(url.value))

            Scenario.eval(result)
          }
        } yield ()

      def help(implicit c: Client[AppTask]): Scenario[AppTask, Unit] =
        for {
          chat <- Scenario.start(command("help").chat)
          _    <- broadcastHelp(chat)
        } yield ()

      def start(implicit c: Client[AppTask]): Scenario[AppTask, Unit] =
        for {
          chat <- Scenario.start(command("start").chat)
          _    <- broadcastHelp(chat)
        } yield ()

      private def broadcastHelp(chat: Chat)(implicit c: Client[AppTask]): Scenario[AppTask, TextMessage] = {
        val helpText =
          """
            |/help Shows this menu
            |/add Subscribe to GitHub project releases
            |/del Delete subscription
            |/list List current subscriptions
            |""".stripMargin

        Scenario.eval(chat.send(helpText))
      }

      override def start(token: String): AppTask[Unit] =
        ZIO
          .runtime[AppEnv]
          .flatMap { implicit rt =>
            Stream
              .resource(Client.global[AppTask](token))
              .flatMap { implicit client =>
                Bot
                  .polling[AppTask]
                  .follow(
                    start,
                    help,
                    addRepository,
                    listRepositories
                  )
              }
              .compile
              .drain
          }
    }
  }
}
