package io.pager

import io.estatico.newtype.macros.newtype
import io.pager.Subscription._

case class Subscription(chatId: ChatId, name: RepositoryName)

object Subscription {
  @newtype case class ChatId(value: Long)
  @newtype case class RepositoryName(value: String)
}
