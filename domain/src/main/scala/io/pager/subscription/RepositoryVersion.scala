package io.pager.subscription

import io.pager.subscription.Repository.{ Name, Version }

final case class RepositoryVersion(name: Name, version: Option[Version])
