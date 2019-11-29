package io.pager.client.github

import java.time.Instant

import io.pager.subscription.Repository.Version

case class GitHubRelease(name: Version, published_at: Instant)
