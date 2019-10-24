package io.pager

import zio.ZIO

package object storage {
  def addProject(project: Repository): ZIO[ProjectReleaseRepository, PagerError, Unit] = ZIO.accessM[ProjectReleaseRepository](_.repository.addProject(project))
  def removeProject(project: Repository): ZIO[ProjectReleaseRepository, PagerError, Unit] = ZIO.accessM[ProjectReleaseRepository](_.repository.removeProject(project))
}
