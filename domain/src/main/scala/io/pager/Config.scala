package io.pager

import io.pager.Config.PagerConfig

final case class Config(releasePager: PagerConfig)

object Config {
  final case class DBConfig(url: String, driver: String, user: String, password: String)
  final case class PagerConfig(dbConfig: DBConfig)

}
