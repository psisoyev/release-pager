package io.pager.subscription

import io.pager.client.telegram.ChatId
import io.pager.subscription.RepositoryStatus.Version

case class RepositoryStatus(version: Version, subscribers: Set[ChatId])
object RepositoryStatus {
  case class Version(value: String)
}
