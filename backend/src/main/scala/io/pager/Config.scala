package io.pager

import io.pager.Config.PagerConfig
import io.pager.PagerError.ConfigurationError
import pureconfig.ConfigSource
import pureconfig.generic.auto._
import zio.{ TaskLayer, ZIO, ZLayer }

final case class Config(releasePager: PagerConfig)

object Config {
  final case class DBConfig(url: String, driver: String, user: String, password: String)
  final case class PagerConfig(dbConfig: DBConfig, botToken: String)

  val live: TaskLayer[Config] = ZLayer {
    ZIO
      .fromEither(ConfigSource.default.load[Config])
      .mapError(e => ConfigurationError(e.prettyPrint()))
  }
}
