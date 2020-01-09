package io.pager.subscription

import io.pager.client.telegram.ChatId
import io.pager.subscription.Repository.{ Name, Version }

final case class Repository(name: Name, version: Version, subscribers: Set[ChatId])

object Repository {
  final case class Version(value: String)
  final case class Name(value: String)
}
