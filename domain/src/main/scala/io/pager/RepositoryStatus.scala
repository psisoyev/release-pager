package io.pager

import io.pager.Subscription.ChatId

case class RepositoryStatus(version: Option[String], subscribers: Set[ChatId])
object RepositoryStatus {
  def empty: RepositoryStatus = RepositoryStatus(None, Set.empty)
}
