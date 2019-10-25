package io.pager.api.http

import cats.effect.Resource
import io.pager.PagerError.{ MalformedUrl, NotFound }
import io.pager.{ AppEnv, AppTask, PagerError }
import org.http4s.Uri
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import zio._
import zio.interop.catz._

import scala.concurrent.ExecutionContext.Implicits

trait HttpClient {
  val httpClient: HttpClient.Service
}

object HttpClient {
  trait Service {
    def get(uri: String): ZIO[AppEnv, PagerError, String]
  }

  trait Http4s extends HttpClient {
    override val httpClient: Service = new Service {
      private val client: ZIO[AppEnv, Nothing, Resource[AppTask, Client[AppTask]]] =
        ZIO
          .runtime[AppEnv]
          .map { implicit rts =>
            BlazeClientBuilder
              .apply[AppTask](Implicits.global)
              .resource
          }

      override def get(uri: String): ZIO[AppEnv, PagerError, String] = {
        def call(uri: Uri): ZIO[AppEnv, PagerError, String] =
          for {
            client <- client
            res <- client
                    .use(_.expect[String](uri))
                    .foldM(_ => ZIO.fail(NotFound(uri.renderString)), ZIO.succeed)
          } yield res

        Uri
          .fromString(uri)
          .fold(_ => ZIO.fail(MalformedUrl(uri)), call)
      }
    }
  }
}
