package io.pager

sealed trait PagerError extends Throwable {
  def message: String
  override def getMessage: String = message
}

object PagerError {
  final case class ConfigurationError(text: String) extends PagerError {
    def message: String = text
  }

  final case object MissingBotToken extends PagerError {
    def message: String = "Bot token is not set as environment variable"
  }

  final case class NotFound(url: String) extends PagerError {
    def message: String = s"$url not found"
  }

  final case class MalformedUrl(url: String) extends PagerError {
    def message: String = s"Couldn't build url for repository: $url"
  }

  final case class UnexpectedError(text: String) extends PagerError {
    def message: String = text
  }

}
