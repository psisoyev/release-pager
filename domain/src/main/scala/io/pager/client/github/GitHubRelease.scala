package io.pager.client.github

import io.pager.subscription.Repository.Version

import java.time.Instant

final case class GitHubRelease(name: Version, published_at: Instant)
