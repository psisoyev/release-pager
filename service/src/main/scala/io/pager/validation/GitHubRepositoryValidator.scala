package io.pager.validation

import cats.effect.Resource
import io.pager.PagerError.{ MalformedRepositoryUrl, RepositoryNotFound }
import io.pager.logger._
import io.pager.{ AppEnv, AppTask, PagerError, Repository }
import org.http4s.Uri
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import zio.ZIO
import zio.interop.catz._

import scala.concurrent.ExecutionContext.Implicits

trait GitHubRepositoryValidator extends RepositoryValidator {
  override val validator: RepositoryValidator.Service = new RepositoryValidator.Service {

    private val httpClient: ZIO[AppEnv, Nothing, Resource[AppTask, Client[AppTask]]] =
      ZIO
        .runtime[AppEnv]
        .map { implicit rts =>
          BlazeClientBuilder
            .apply[AppTask](Implicits.global)
            .resource
        }

    def validate(name: String): ZIO[AppEnv, PagerError, Repository.Name] = {
      def repositoryExists(uri: Uri) = for {
        httpClient <- httpClient
        res <- httpClient
          .use(_.expect[String](uri / "releases"))
          .foldM(failure, success)
      } yield res

      def failure(e: Throwable) =
        info(s"Failed to find repository $name") *> ZIO.fail(RepositoryNotFound(name))

      def success(s: String) =
        info(s"Validated repository $name") *> ZIO.succeed(Repository.Name(name))

      val url =
        if (name.startsWith("http")) name
        else s"https://github.com/$name"

      Uri
        .fromString(url)
        .fold(_ => ZIO.fail(MalformedRepositoryUrl(name)), repositoryExists)
    }
  }
}
