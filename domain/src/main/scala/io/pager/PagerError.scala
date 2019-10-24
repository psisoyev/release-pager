package io.pager

trait PagerError extends Throwable {
  def message: String
}

object PagerError {
  case class RepositoryNotFound(name: String) extends PagerError {
    def message: String = s"Repository $name not found"
  }

  case class MalformedRepositoryUrl(name: String) extends PagerError {
    def message: String = s"Couldn't build repository $name url"
  }

  case class UnexpectedError(text: String) extends PagerError {
    def message: String = text
  }

}
