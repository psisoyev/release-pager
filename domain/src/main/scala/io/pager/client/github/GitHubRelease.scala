package io.pager.client.github

import java.time.Instant

import io.pager.subscription.RepositoryStatus.Version

case class GitHubRelease(name: Version, published_at: Instant)
