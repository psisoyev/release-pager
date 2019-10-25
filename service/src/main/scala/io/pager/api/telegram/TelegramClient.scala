package io.pager.api.telegram

import canoe.api.{ TelegramClient => Client, _ }
import canoe.syntax._
import fs2.Stream
import io.pager.validation.RepositoryValidator
import io.pager.{ AppEnv, AppTask }
import zio._
import zio.interop.catz._

trait TelegramClient {
  val telegramClient: TelegramClient.Service
}

object TelegramClient {
  trait Service {
    def start(token: String): AppTask[Int]
  }

  trait Canoe extends TelegramClient {
    self: RepositoryValidator =>
    override val telegramClient: TelegramClient.Service = new TelegramClient.Service {
      def addRepository(implicit c: Client[AppTask]): Scenario[AppTask, Unit] =
        for {
          chat       <- Scenario.start(command("add").chat)
          _          <- Scenario.eval(chat.send("Please provide a link to the repository or it's full name"))
          repository <- Scenario.next(text)
          _          <- Scenario.eval(chat.send(s"Checking repository $repository"))
          validationResult = validator
            .validate(repository)
            .foldM(
              e => chat.send(s"Couldn't add repository $repository: ${e.message}"),
              _ => chat.send(s"Added repository $repository")
            )
          _ <- Scenario.eval(validationResult)
        } yield ()

      def help[F[_]: Client]: Scenario[F, Unit] = {
        val helpText =
          """
            |/help Shows this menu
            |/add Subscribe to GitHub project releases
            |/del Delete subscription
            |/list List current subscriptions
            |""".stripMargin

        for {
          chat <- Scenario.start(command("help").chat)
          _    <- Scenario.eval(chat.send(helpText))
        } yield ()
      }

      def start(token: String): AppTask[Int] =
        ZIO
          .runtime[AppEnv]
          .flatMap { implicit rt =>
            Stream
              .resource(Client.global[AppTask](token))
              .flatMap { implicit client =>
                Bot.polling[AppTask].follow(help, addRepository)
              }
              .compile
              .drain
              .as(1)
          }
    }
  }
}
