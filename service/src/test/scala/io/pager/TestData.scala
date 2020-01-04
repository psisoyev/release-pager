package io.pager

import java.time.Instant

import io.pager.client.github.GitHubRelease
import io.pager.client.telegram.ChatId
import io.pager.subscription.Repository.{ Name, Version }

object TestData {
  val finalVersion: Version = Version("1.0.0")
  val rcVersion: Version    = Version("1.0.0-RC17")

  val timestamp1: Instant = Instant.parse("2019-11-29T17:34:23.00Z")
  val timestamp2: Instant = Instant.parse("2019-12-30T18:35:24.00Z")

  val rcRelease: GitHubRelease      = GitHubRelease(rcVersion, timestamp1)
  val finalRelease: GitHubRelease   = GitHubRelease(finalVersion, timestamp2)
  val releases: List[GitHubRelease] = List(rcRelease, finalRelease)

  val chatId1: ChatId          = ChatId(1L)
  val chatId2: ChatId          = ChatId(2L)
  val subscribers: Set[ChatId] = Set(chatId1, chatId2)

  def message(name: Name) = s"There is a new version of ${name.value} available: ${finalVersion.value}"
}
