package io.pager

import io.pager.api.telegram.TelegramClient
import io.pager.logger._
import io.pager.validation.GitHubRepositoryValidator
import zio.clock.Clock
import zio.console.{ putStrLn, Console }
import zio.{ ZEnv, _ }

object Main extends zio.App {
  override def run(args: List[String]): ZIO[ZEnv, Nothing, Int] = {
    val token = "XXX"

    val result: ZIO[ZEnv, Throwable, Unit] = for {
      _ <- putStrLn("Starting bot")

      program     = ZIO.environment[AppEnv].flatMap(_.telegramClient.start(token))

      _ <- program.provideSome[ZEnv] { base =>
            new Clock with Console with TelegramClient.Canoe with GitHubRepositoryValidator with ConsoleLogger {
              override val clock: Clock.Service[Any]     = base.clock
              override val console: Console.Service[Any] = base.console
            }
          }
    } yield ()

    result.foldM(
      err => putStrLn(s"Execution failed with: $err") *> ZIO.succeed(1),
      _ => ZIO.succeed(0)
    )
  }
}
