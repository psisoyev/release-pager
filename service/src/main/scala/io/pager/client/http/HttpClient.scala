package io.pager.client.http

import io.circe.{ Decoder, Encoder }
import io.pager.PagerError
import io.pager.PagerError.{ MalformedUrl, NotFound }
import org.http4s._
import org.http4s.circe._
import org.http4s.client.Client
import zio.ZLayer.NoDeps
import zio.{ Task, _ }
import zio.interop.catz._
import io.pager.client.http.Http4s

object HttpClient {
  type HttpClient = Has[Service]

  trait Service {
    def get[T](uri: String)(implicit d: Decoder[T]): IO[PagerError, T]
  }

  def http4s: ZLayer[Has[Client[Task]], Nothing, Has[Service]] =
    ZLayer.fromService[Client[Task], Service] { http4sClient: Client[Task] =>
      Http4s(http4sClient)
    }
}
