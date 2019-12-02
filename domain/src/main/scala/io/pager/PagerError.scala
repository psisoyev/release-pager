package io.pager

trait PagerError extends Throwable {
  def message: String
}

object PagerError {
  case object MissingBotToken extends PagerError {
    def message: String = s"Bot token is not set as environment variable"
  }

  case class NotFound(url: String) extends PagerError {
    def message: String = s"$url not found"
  }

  case class MalformedUrl(url: String) extends PagerError {
    def message: String = s"Couldn't build url for repository: $url"
  }

  case class UnexpectedError(text: String) extends PagerError {
    def message: String = text
  }

}
