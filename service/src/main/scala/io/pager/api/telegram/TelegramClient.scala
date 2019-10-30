package io.pager.api.telegram

import canoe.api.{ TelegramClient => Client, _ }
import canoe.models.Chat
import canoe.models.messages.TextMessage
import canoe.syntax._
import fs2.Stream
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
    def start(token: String): ClientTask[Unit]
  }

  trait Canoe extends TelegramClient.Service {
    def addRepository(implicit c: Client[ClientTask]): Scenario[ClientTask, Unit] =
      for {
        chat      <- Scenario.start(command("add").chat)
        _         <- Scenario.eval(chat.send("Please provide a link to the repository or it's 'organization/name'"))
        _         <- Scenario.eval(chat.send("Examples: 'https://github.com/zio/zio' or 'zio/zio'"))
        userInput <- Scenario.next(text)
        _         <- Scenario.eval(chat.send(s"Checking repository $userInput"))
        _ <- Scenario.eval(
              validate(userInput)
                .foldM(
                  e => chat.send(s"Couldn't add repository $userInput: ${e.message}"),
                  url => chat.send(s"Added repository $userInput") *> subscribe(Subscription(ChatId(chat.id), url))
                )
            )
      } yield ()

    def listRepositories(implicit c: Client[ClientTask]): Scenario[ClientTask, Unit] =
      for {
        chat  <- Scenario.start(command("list").chat)
        repos <- Scenario.eval(ZIO.environment[ClientEnv] *> list(ChatId(chat.id)))
        _ <- {
          val result =
            if (repos.isEmpty) chat.send("You don't have subscriptions yet")
            else chat.send("Listing your subscriptions:") *> ZIO.foreach(repos)(url => chat.send(url.value))

          Scenario.eval(result)
        }
      } yield ()

    def help(implicit c: Client[ClientTask]): Scenario[ClientTask, Unit] =
      for {
        chat <- Scenario.start(command("help").chat)
        _    <- broadcastHelp(chat)
      } yield ()

    def start(implicit c: Client[ClientTask]): Scenario[ClientTask, Unit] =
      for {
        chat <- Scenario.start(command("start").chat)
        _    <- broadcastHelp(chat)
      } yield ()

    private def broadcastHelp(chat: Chat)(implicit c: Client[ClientTask]): Scenario[ClientTask, TextMessage] = {
      val helpText =
        """
          |/help Shows this menu
          |/add Subscribe to GitHub project releases
          |/del Delete subscription
          |/list List current subscriptions
          |""".stripMargin

      Scenario.eval(chat.send(helpText))
    }

    override def start(token: String): ClientTask[Unit] =
      ZIO
        .runtime[ClientEnv]
        .flatMap { implicit rt =>
          Stream
            .resource(Client.global[ClientTask](token))
            .flatMap { implicit client =>
              Bot
                .polling[ClientTask]
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
