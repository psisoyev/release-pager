package io.pager.api.http

import cats.effect.Resource
import io.pager.PagerError
import io.pager.PagerError.{ MalformedUrl, NotFound }
import org.http4s.Uri
import org.http4s.client.Client
import zio._
import zio.interop.catz._

trait HttpClient {
  val httpClient: HttpClient.Service
}

object HttpClient {
  trait Service {
    def get(uri: String): IO[PagerError, String]
  }

  trait Http4s extends HttpClient {
    def client: Resource[Task, Client[Task]]

    override val httpClient: Service = new Service {
      override def get(uri: String): IO[PagerError, String] = {
        def call(uri: Uri): IO[PagerError, String] =
          client
            .use(_.expect[String](uri))
            .foldM(_ => IO.fail(NotFound(uri.renderString)), ZIO.succeed)

        Uri
          .fromString(uri)
          .fold(_ => IO.fail(MalformedUrl(uri)), call)
      }
    }
  }
}
