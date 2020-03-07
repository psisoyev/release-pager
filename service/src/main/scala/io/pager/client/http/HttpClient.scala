package io.pager.client.http

import io.circe.{ Decoder, Encoder }
import io.pager.PagerError
import io.pager.PagerError.{ MalformedUrl, NotFound }
import org.http4s._
import org.http4s.circe._
import org.http4s.client.Client
import zio._
import zio.interop.catz._

object HttpClient {
  type HttpClient = Has[Service]

  trait Service {
    def get[T](uri: String)(implicit d: Decoder[T]): IO[PagerError, T]
  }

  final case class Http4s(client: Client[Task]) extends Service {
    implicit def entityDecoder[A](implicit decoder: Decoder[A]): EntityDecoder[Task, A] = jsonOf[Task, A]
    implicit def entityEncoder[A](implicit encoder: Encoder[A]): EntityEncoder[Task, A] = jsonEncoderOf[Task, A]

    def get[T](uri: String)(implicit d: Decoder[T]): IO[PagerError, T] = {
      def call(uri: Uri): IO[PagerError, T] =
        client
          .expect[T](uri)
          .foldM(_ => IO.fail(NotFound(uri.renderString)), result => ZIO.succeed(result))

      Uri
        .fromString(uri)
        .fold(_ => IO.fail(MalformedUrl(uri)), call)
    }
  }

  val http4s: ZLayer[Client[Task], Nothing, Has[Http4s]] =
    ZLayer.fromFunction { http4sClient: Client[Task] =>
      Http4s(http4sClient)
    }
}
