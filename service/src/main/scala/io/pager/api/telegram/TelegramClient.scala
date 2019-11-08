package io.pager.api.telegram

import canoe.api.models.ChatApi
import canoe.api.{ TelegramClient => Client, _ }
import canoe.models.messages.TextMessage
import canoe.models.outgoing.TextContent
import canoe.models.{ Chat, PrivateChat }
import canoe.syntax._
import io.pager.Subscription.ChatId
import io.pager.storage._
import io.pager.validation._
import io.pager.{ ClientEnv, Subscription }
import zio._
import zio.interop.catz._

trait TelegramClient {
  val telegramClient: TelegramClient.Service
}

object TelegramClient {
  type ClientTask[A] = RIO[ClientEnv, A]

  trait Service {
    def start: ClientTask[Unit]
  }

  class Canoe()(implicit canoeClient: Client[ClientTask]) extends TelegramClient {
    override val telegramClient: Service = new Service {
      def addRepository: Scenario[ClientTask, Unit] =
        for {
          chat      <- Scenario.start(command("add").chat)
          _         <- Scenario.eval(chat.send("Please provide repository in form 'organization/name'"))
          _         <- Scenario.eval(chat.send("Examples: psisoyev/release-pager or zio/zio"))
          userInput <- Scenario.next(text)
          _         <- Scenario.eval(chat.send(s"Checking repository $userInput"))
          _ <- Scenario.eval(
                validate(userInput)
                  .foldM(
                    e => chat.send(s"Couldn't add repository $userInput: ${e.message}"),
                    name => chat.send(s"Added repository $userInput") *> subscribe(Subscription(ChatId(chat.id), name))
                  )
              )
        } yield ()

      def listRepositories: Scenario[ClientTask, Unit] =
        for {
          chat  <- Scenario.start(command("list").chat)
          repos <- Scenario.eval(ZIO.environment[ClientEnv] *> list(ChatId(chat.id)))
          _ <- {
            val result =
              if (repos.isEmpty) chat.send("You don't have subscriptions yet")
              else chat.send("Listing your subscriptions:") *> ZIO.foreach(repos)(name => chat.send(name.value))

            Scenario.eval(result)
          }
        } yield ()

      def help: Scenario[ClientTask, Unit] =
        for {
          chat <- Scenario.start(command("help").chat)
          _    <- broadcastHelp(chat)
        } yield ()

      def startBot: Scenario[ClientTask, Unit] =
        for {
          chat <- Scenario.start(command("start").chat)
          _    = println(chat)
          _    <- broadcastHelp(chat)
        } yield ()

      private def broadcastHelp(chat: Chat): Scenario[ClientTask, TextMessage] = {
        val helpText =
          """
            |/help Shows this menu
            |/add Subscribe to GitHub project releases
            |/del Delete subscription
            |/list List current subscriptions
            |""".stripMargin

        Scenario.eval(chat.send(helpText))
      }

      override def start: ClientTask[Unit] = {
        Bot
          .polling[ClientTask]
          .follow(startBot, help, addRepository, listRepositories)
          .compile
          .drain
      }
    }
  }
}
