package io.pager.client.telegram.scenario

import canoe.api._
import canoe.models.Chat
import canoe.models.messages.TextMessage
import canoe.syntax._
import io.pager.subscription.SubscriptionLogic
import io.pager.validation.RepositoryValidator
import zio._
import io.pager.PagerError
import io.pager.subscription.Repository.Name
import io.pager.client.telegram.ChatId
import io.pager.client.telegram.scenario.CanoeScenarios

private[scenario] final case class Live(
  repositoryValidator: RepositoryValidator.Service,
  subscriptionLogic: SubscriptionLogic.Service,
  canoeClient: TelegramClient[Task]
) extends CanoeScenarios.Service {

  private implicit val client: TelegramClient[Task] = canoeClient

  override def add: Scenario[Task, Unit] =
    for {
      chat      <- Scenario.expect(command("add").chat)
      _         <- Scenario.eval(chat.send("Please provide repository in form 'organization/name'"))
      _         <- Scenario.eval(chat.send("Examples: psisoyev/release-pager or zio/zio"))
      userInput <- Scenario.expect(text)
      _         <- Scenario.eval(chat.send(s"Checking repository '$userInput'"))
      _         <- Scenario.eval(subscribe(chat, userInput, validate(userInput)))
    } yield ()

  private def validate(userInput: String): IO[PagerError, Name] =
    repositoryValidator.validate(userInput)

  private def subscribe(chat: Chat, userInput: String, validated: IO[PagerError, Name]): Task[Unit] =
    validated
      .foldM(
        e => chat.send(s"Couldn't add repository '$userInput': ${e.message}"),
        name => chat.send(s"Added repository '$userInput'") *> subscriptionLogic.subscribe(ChatId(chat.id), name)
      )
      .unit

  override def del: Scenario[Task, Unit] =
    for {
      chat      <- Scenario.expect(command("del").chat)
      _         <- Scenario.eval(chat.send("Please provide repository in form 'organization/name'"))
      _         <- Scenario.eval(chat.send("Examples: psisoyev/release-pager or zio/zio"))
      userInput <- Scenario.expect(text)
      _         <- Scenario.eval(chat.send(s"Checking repository '$userInput'"))
      _ <- Scenario.eval {
            subscriptionLogic.unsubscribe(ChatId(chat.id), Name(userInput)) *>
              chat.send(s"Removed repository '$userInput' from your subscription list")
          }
    } yield ()

  override def list: Scenario[Task, Unit] =
    for {
      chat  <- Scenario.expect(command("list").chat)
      repos <- Scenario.eval(subscriptionLogic.listSubscriptions(ChatId(chat.id)))
      _ <- {
        val result =
          if (repos.isEmpty) chat.send("You don't have subscriptions yet")
          else chat.send("Listing your subscriptions:") *> ZIO.foreach(repos)(name => chat.send(name.value))

        Scenario.eval(result)
      }
    } yield ()

  override def help: Scenario[Task, Unit] =
    for {
      chat <- Scenario.expect(command("help").chat)
      _    <- broadcastHelp(chat)
    } yield ()

  override def start: Scenario[Task, Unit] =
    for {
      chat <- Scenario.expect(command("start").chat)
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
