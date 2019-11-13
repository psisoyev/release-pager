package io.pager

import io.pager.RepositoryStatus.Version
import io.pager.Subscription.ChatId

case class RepositoryStatus(version: Option[Version], subscribers: Set[ChatId]) {
  def newVersion(version: Version): RepositoryStatus = copy(Some(version))
}
object RepositoryStatus {
  case class Version(value: String)
  def empty: RepositoryStatus = RepositoryStatus(None, Set.empty)
}
