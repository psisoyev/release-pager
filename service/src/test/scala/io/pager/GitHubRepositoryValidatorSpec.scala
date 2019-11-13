package io.pager

import io.pager.PagerError.NotFound
import io.pager.Subscription.RepositoryName
import io.pager.api.github.GitHubClient
import io.pager.logging.Logger
import io.pager.validation.{ GitHubRepositoryValidator, RepositoryValidator }
import zio._
import zio.test.Assertion._
import zio.test._
import GitHubRepositoryValidatorTestCases._

object GitHubRepositoryValidatorSpec extends DefaultRunnableSpec(suite(specName)(seq: _*))

object GitHubRepositoryValidatorTestCases {
  val specName: String = "GitHubRepositoryValidatorSpec"

  private def buildValidator(client: GitHubClient.Service): RepositoryValidator.Service =
    new GitHubRepositoryValidator {
      override val logger: Logger.Service             = Logger.Test
      override val gitHubClient: GitHubClient.Service = client
    }.repositoryValidator

  private val notFound = NotFound("yourRepo")

  private val succeedingValidator = buildValidator {
    new GitHubClient.Service {
      override def repositoryExists(name: RepositoryName): IO[PagerError, RepositoryName] = IO.succeed(name)
      override def releases(name: RepositoryName): IO[PagerError, List[GitHubRelease]]    = ???
    }
  }
  private val failingValidator = buildValidator(???)

  val seq = Seq(
    testM("successfully validate existing repository by name") {
      val repo = "zio/zio"

      succeedingValidator
        .validate(repo)
        .map(result => assert(result, equalTo(RepositoryName("zio/zio"))))
        .provide(succeedingValidator)
    },
    testM("fail to validate non-existing portfolio") {
      val repo = "ololo"

      failingValidator
        .validate(repo)
        .flip
        .map(result => assert(result, equalTo(notFound)))
        .provide(failingValidator)
    }
  )
}
