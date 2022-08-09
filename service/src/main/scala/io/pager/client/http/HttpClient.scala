package io.pager.client.http

import io.circe.Decoder
import io.pager.PagerError
import org.http4s.client.Client
import zio.{ IO, Task, URLayer, ZIO, ZLayer }

trait HttpClient {
  def get[T](uri: String)(implicit d: Decoder[T]): IO[PagerError, T]
}

object HttpClient {
  def http4s: URLayer[Client[Task], HttpClient] = ZLayer {
    ZIO.service[Client[Task]].map(Http4s(_))
  }
}
