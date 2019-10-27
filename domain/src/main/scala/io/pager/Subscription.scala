package io.pager

import io.estatico.newtype.macros.newtype
import io.pager.Subscription._

case class Subscription(chatId: ChatId, url: RepositoryUrl)

object Subscription {
  @newtype case class ChatId(value: Long)
  @newtype case class RepositoryUrl(value: String)
}
