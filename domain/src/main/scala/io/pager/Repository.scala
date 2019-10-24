package io.pager

import io.estatico.newtype.macros.newtype
import io.pager.Repository._

case class Repository(id: Id, name: Name)

object Repository {
  @newtype case class Id(value: String)
  @newtype case class Name(value: String)
}