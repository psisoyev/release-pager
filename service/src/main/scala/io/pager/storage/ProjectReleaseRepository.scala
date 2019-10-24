package io.pager.storage

import io.pager.{PagerError, Repository}
import zio.ZIO

trait ProjectReleaseRepository {
  val repository: ProjectReleaseRepository.Service
}

object ProjectReleaseRepository {
  trait Service {
    def addProject(project: Repository): ZIO[Any, PagerError, Unit]
    def removeProject(project: Repository): ZIO[Any, PagerError, Unit]
  }
}
