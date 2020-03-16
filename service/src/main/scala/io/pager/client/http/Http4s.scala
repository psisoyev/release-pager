package io.pager.client.http

import org.http4s.EntityDecoder
import io.circe.Decoder
import org.http4s._
import org.http4s.circe._
import org.http4s.client.Client
import io.pager.PagerError.NotFound
import io.pager.PagerError.MalformedUrl
import zio.Task
import io.circe.Encoder
import zio.IO
import io.pager.PagerError
import zio.ZIO
import zio.interop.catz._

private[http] final case class Http4s(client: Client[Task]) extends HttpClient.Service {
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
