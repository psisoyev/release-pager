package io.pager.storage
import io.pager.{PagerError, Repository}
import zio.ZIO

trait DoobieProjectReleaseRepository extends ProjectReleaseRepository {
  override val repository: ProjectReleaseRepository.Service = new ProjectReleaseRepository.Service {
    override def addProject(project: Repository): ZIO[Any, PagerError, Unit] = ???
    override def removeProject(project: Repository): ZIO[Any, PagerError, Unit] = ???
  }
}
