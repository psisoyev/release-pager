package io.pager.validation

import cats.effect.Resource
import io.pager.PagerError.{MalformedRepositoryUrl, RepositoryNotFound}
import io.pager.logger._
import io.pager.{AppEnv, AppTask, PagerError, Repository}
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
      val url =
        if (name.startsWith("http")) name
        else s"https://github.com/$name"

      Uri
        .fromString(url)
        .fold(
          _ => ZIO.fail(MalformedRepositoryUrl(name)),
          uri => {
            for {
              httpClient <- httpClient
              res <- httpClient
                      .use(_.expect[String](uri))
                      .mapError(_ => RepositoryNotFound(name))
              _ <- info(s"Validated repository $name")
            } yield Repository.Name(res)
          }
        )
    }
  }
}
