package io.pager

import canoe.api._
import canoe.syntax._
import io.pager.logger._
import zio.{UIO, ZIO}

object Scenarios {
//  def addRepository[F[_] : TelegramClient]: Scenario[F, Unit] = {
//
//    for {
//      chat <- Scenario.start[Task, Chat](command("add").chat)
//      _ <- Scenario.eval[Task, TextMessage](chat.send("Please provide a link to the repository or it's full name"))
//      repository <- Scenario.next[Task, String](text)
//    } yield ()
//
//
//    val repo = {
//      val scenario = for {
//        chat <- Scenario.start(command("add").chat)
//        _ <- Scenario.eval(chat.send("Please provide a link to the repository or it's full name"))
//        repository <- Scenario.next(text)
//        _ <- Scenario.eval(chat.send(s"Checking repository $repository"))
//      } yield repository
//
//      ZIO.succeed(scenario)
//    }
//
//    for {
//      repository <- repo
//      _ <- validate(repository)
//      _ <- Scenario.eval(chat.send(s"Checking repository $repository"))
//    } yield ()
//  }

  def help[F[_] : TelegramClient]: Scenario[F, Unit] = {
    val helpText =
      """
        |/help Shows this menu
        |/add Subscribe to GitHub project releases
        |/del Delete subscription
        |/list List current subscriptions
        |""".stripMargin

    for {
      chat <- Scenario.start(command("help").chat)
      _ <- Scenario.eval(chat.send(helpText))
    } yield ()
  }

  def processError(e: Throwable): ZIO[Logger, PagerError, String] =
    error(e)("Couldn't process command") *> UIO.succeed("Something went wrong while adding new project. Please try again.")
}
