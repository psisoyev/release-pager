package io.pager.client.telegram

import canoe.api.{ TelegramClient => Client, _ }
import canoe.models.Chat
import canoe.models.messages.TextMessage
import canoe.syntax._
import io.pager.subscription.Repository.Name
import io.pager.subscription.SubscriptionLogic
import io.pager.validation.RepositoryValidator
import zio._

trait ScenarioLogic[Scenario[F[_], _]] {
  val scenarios: ScenarioLogic.Service[Scenario]
}

object ScenarioLogic {
  trait Service[Scenario[F[_], _]] {
    def start: Scenario[Task, Unit]
    def help: Scenario[Task, Unit]

    def add: Scenario[Task, Unit]
    def del: Scenario[Task, Unit]
    def list: Scenario[Task, Unit]
  }

  trait CanoeScenarios extends ScenarioLogic[Scenario] {
    implicit def canoeClient: Client[Task]
    def subscription: SubscriptionLogic.Service
    def repositoryValidator: RepositoryValidator.Service

    override val scenarios: Service[Scenario] = new Service[Scenario] {
      override def add: Scenario[Task, Unit] =
        for {
          chat      <- Scenario.start(command("add").chat)
          _         <- Scenario.eval(chat.send("Please provide repository in form 'organization/name'"))
          _         <- Scenario.eval(chat.send("Examples: psisoyev/release-pager or zio/zio"))
          userInput <- Scenario.next(text)
          _         <- Scenario.eval(chat.send(s"Checking repository '$userInput'"))
          _ <- Scenario.eval(
                repositoryValidator
                  .validate(userInput)
                  .foldM(
                    e => chat.send(s"Couldn't add repository '$userInput': ${e.message}"),
                    name => chat.send(s"Added repository '$userInput'") *> subscription.subscribe(ChatId(chat.id), name)
                  )
              )
        } yield ()

      override def del: Scenario[Task, Unit] =
        for {
          chat      <- Scenario.start(command("del").chat)
          _         <- Scenario.eval(chat.send("Please provide repository in form 'organization/name'"))
          _         <- Scenario.eval(chat.send("Examples: psisoyev/release-pager or zio/zio"))
          userInput <- Scenario.next(text)
          _         <- Scenario.eval(chat.send(s"Checking repository '$userInput'"))
          _ <- Scenario.eval {
                subscription.unsubscribe(ChatId(chat.id), Name(userInput)) *>
                  chat.send(s"Removed repository '$userInput' from your subscription list")
              }
        } yield ()

      override def list: Scenario[Task, Unit] =
        for {
          chat  <- Scenario.start(command("list").chat)
          repos <- Scenario.eval(subscription.listSubscriptions(ChatId(chat.id)))
          _ <- {
            val result =
              if (repos.isEmpty) chat.send("You don't have subscriptions yet")
              else chat.send("Listing your subscriptions:") *> ZIO.foreach(repos)(name => chat.send(name.value))

            Scenario.eval(result)
          }
        } yield ()

      override def help: Scenario[Task, Unit] =
        for {
          chat <- Scenario.start(command("help").chat)
          _    <- broadcastHelp(chat)
        } yield ()

      override def start: Scenario[Task, Unit] =
        for {
          chat <- Scenario.start(command("start").chat)
          _    <- broadcastHelp(chat)
        } yield ()

      private def broadcastHelp(chat: Chat): Scenario[Task, TextMessage] = {
        val helpText =
          """
            |/help Shows help menu
            |/add Subscribe to GitHub project releases
            |/del Unsubscribe from GitHub project releases
            |/list List current subscriptions
            |""".stripMargin

        Scenario.eval(chat.send(helpText))
      }
    }
  }
}
