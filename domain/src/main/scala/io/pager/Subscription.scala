package io.pager

import io.estatico.newtype.macros.newtype
import io.pager.Subscription._

case class Subscription(id: ChatId, name: RepositoryUrl)

object Subscription {
  @newtype case class ChatId(value: String)
  @newtype case class RepositoryUrl(value: String)
}
