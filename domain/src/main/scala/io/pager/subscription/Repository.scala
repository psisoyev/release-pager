package io.pager.subscription

import io.pager.client.telegram.ChatId
import io.pager.subscription.Repository.{ Name, Version }

case class Repository(name: Name, version: Version, subscribers: Set[ChatId])

object Repository {
  case class Version(value: String)
  case class Name(value: String)
}
