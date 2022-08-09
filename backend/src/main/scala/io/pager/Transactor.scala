package io.pager

import doobie.hikari.HikariTransactor
import zio.interop.catz._
import zio.{ Scope, Task, URLayer, ZIO, ZLayer }

import scala.concurrent.ExecutionContext

object Transactor {
  val live: URLayer[Scope with Config, HikariTransactor[Task]] = ZLayer {
    ZIO.service[Config].flatMap { config =>
      val cfg = config.releasePager.dbConfig
      HikariTransactor
        .newHikariTransactor[Task](
          cfg.driver,
          cfg.url,
          cfg.user,
          cfg.password,
          ExecutionContext.global
        )
        .toScopedZIO
        .orDie
    }
  }
}
