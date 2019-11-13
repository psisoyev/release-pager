package io.pager

import io.pager.Subscription._

case class Subscription(chatId: ChatId, name: RepositoryName)

object Subscription {
  case class ChatId(value: Long)
  case class RepositoryName(value: String)
}
