package io.pager.subscription.repository

import io.pager.subscription.Repository.{ Name, Version }
import io.pager.subscription.repository.RepositoryVersionStorage.RepositoryVersionMap
import zio.{ Ref, UIO }

private[repository] final case class InMemory(versions: Ref[RepositoryVersionMap]) extends RepositoryVersionStorage {
  override def listRepositories: UIO[RepositoryVersionMap] = versions.get

  override def addRepository(name: Name): UIO[Unit] =
    versions
      .update(_ + (name -> None))
      .unit

  override def deleteRepository(name: Name): UIO[Unit] =
    versions
      .update(_ - name)
      .unit

  override def updateVersion(name: Name, version: Version): UIO[Unit] =
    versions
      .update(_ + (name -> Some(version)))
      .unit
}
