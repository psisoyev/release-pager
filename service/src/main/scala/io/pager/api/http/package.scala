package io.pager.api

import io.pager.PagerError
import zio.ZIO

package object http {
  def get(uri: String): ZIO[HttpClient, PagerError, String] = ZIO.accessM[HttpClient](_.httpClient.get(uri))
}
