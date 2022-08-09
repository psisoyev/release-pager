package io.pager.client.http

import io.circe.{ Decoder, Encoder }
import io.pager.PagerError
import io.pager.PagerError.{ MalformedUrl, NotFound }
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s._
import zio.interop.catz._
import zio.{ IO, Task, ZIO }

private[http] final case class Http4s(client: Client[Task]) extends HttpClient {
  implicit def entityDecoder[A](implicit decoder: Decoder[A]): EntityDecoder[Task, A] = jsonOf[Task, A]
  implicit def entityEncoder[A](implicit encoder: Encoder[A]): EntityEncoder[Task, A] = jsonEncoderOf[Task, A]

  def get[T](uri: String)(implicit d: Decoder[T]): IO[PagerError, T] = {
    def call(uri: Uri): IO[PagerError, T] =
      client
        .expect[T](uri)
        .foldZIO(_ => ZIO.fail(NotFound(uri.renderString)), result => ZIO.succeed(result))

    Uri
      .fromString(uri)
      .fold(_ => ZIO.fail(MalformedUrl(uri)), call)
  }
}
