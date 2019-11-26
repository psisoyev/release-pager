package io.pager.subscription

import io.pager.api.telegram.ChatId
import io.pager.subscription.RepositoryStatus.Version

case class RepositoryStatus(version: Option[Version], subscribers: Set[ChatId]) {
  def newVersion(version: Version): RepositoryStatus = copy(Some(version))
}
object RepositoryStatus {
  case class Version(value: String)
  def empty: RepositoryStatus = RepositoryStatus(None, Set.empty)
}
