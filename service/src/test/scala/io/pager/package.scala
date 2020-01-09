package io

import zio.random.Random
import zio.test.{ Sized, ZSpec }

package object pager {
  type TestScenarios = List[ZSpec[Random with Sized, Throwable, String, Unit]]
}
