package io.pager

import org.flywaydb.core.Flyway
import zio.{ Task, URLayer, ZIO, ZLayer }

trait FlywayMigration {
  def migrate: Task[Unit]
}

object FlywayMigration {
  val live: URLayer[Config, FlywayMigration] = ZLayer {
    ZIO.service[Config].map { config =>
      val cfg = config.releasePager.dbConfig
      new FlywayMigration {
        override def migrate: Task[Unit] =
          ZIO.attempt {
            Flyway
              .configure(this.getClass.getClassLoader)
              .dataSource(cfg.url, cfg.user, cfg.password)
              .locations("migrations")
              .connectRetries(Int.MaxValue)
              .load()
              .migrate()
          }.unit
      }
    }
  }
}
