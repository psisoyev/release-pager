package io.pager

import io.pager.subscription.Repository.Name
import zio.random.Random
import zio.test.{ Gen, Sized }
import zio.test.Gen._

object Generators {
  val repositoryName: Gen[Random with Sized, Name] = anyString.map(Name.apply)
}
