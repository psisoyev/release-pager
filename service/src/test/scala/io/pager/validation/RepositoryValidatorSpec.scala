package io.pager.validation

import io.pager.PagerError
import io.pager.PagerError.NotFound
import io.pager.TestCases.TestScenarios
import io.pager.client.github.{ GitHubClient, GitHubRelease }
import io.pager.logging.Logger
import io.pager.subscription.Repository.Name
import io.pager.validation.RepositoryValidator.GitHub
import io.pager.validation.RepositoryValidatorTestCases._
import zio._
import zio.test.Assertion._
import zio.test._

object RepositoryValidatorSpec extends DefaultRunnableSpec(suite(specName)(scenarios: _*))

object RepositoryValidatorTestCases {
  val specName: String = "RepositoryValidatorSpec"

  private def makeValidator(client: GitHubClient.Service[Any]): RepositoryValidator.Service =
    new GitHub {
      override val logger: Logger.Service                  = Logger.Test
      override val gitHubClient: GitHubClient.Service[Any] = client
    }.repositoryValidator

  private val notFound = NotFound("ololo")

  private val succeedingValidator = makeValidator {
    new GitHubClient.Service[Any] {
      override def repositoryExists(name: Name): IO[PagerError, Name]        = IO.succeed(name)
      override def releases(name: Name): IO[PagerError, List[GitHubRelease]] = ???
    }
  }
  private val failingValidator = makeValidator {
    new GitHubClient.Service[Any] {
      override def repositoryExists(name: Name): IO[PagerError, Name]        = IO.fail(notFound)
      override def releases(name: Name): IO[PagerError, List[GitHubRelease]] = ???
    }
  }

  val scenarios = List(
    testM("successfully validate existing repository by name") {
      val repo   = "zio/zio"
      val result = succeedingValidator.validate(repo)

      assertM(result, equalTo(Name("zio/zio")))
    },
    testM("fail to validate non-existing portfolio") {
      val repo = "ololo"
      val result = failingValidator
        .validate(repo)
        .flip

      assertM(result, equalTo(notFound))
    }
  )
}
