package io.pager.validation

import io.pager.PagerError.NotFound
import io.pager.client.github.GitHubClientMock
import io.pager.log.Logger
import io.pager.subscription.Repository.Name
import zio.Task
import zio.test.Assertion._
import zio.test._
import zio.test.mock.Expectation._

object RepositoryValidatorSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[Environment, Failure] = suite("RepositoryValidatorSpec")(
    testM("successfully validate existing repository by name") {
      val repo             = Name("zio/zio")
      val gitHubClientMock = GitHubClientMock.RepositoryExists(equalTo(repo), value(repo))
      val layer            = (Logger.silent ++ gitHubClientMock) >>> RepositoryValidator.live

      assertM(RepositoryValidator.validate(repo.value))(equalTo(repo)).provideLayer(layer)
    },
    testM("fail to validate non-existing portfolio") {
      val repo     = Name("ololo")
      val notFound = NotFound("ololo")

      val gitHubClientMock = GitHubClientMock.RepositoryExists(equalTo(repo), failure(notFound))
      val layer            = (Logger.silent ++ gitHubClientMock) >>> RepositoryValidator.live

      assertM(RepositoryValidator.validate(repo.value).flip)(equalTo(notFound)).provideLayer(layer)
    }
  )
}
