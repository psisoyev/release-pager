package io.pager

import io.pager.TestCases.{ Suite, TestScenarios }
import zio.random.Random
import zio.test
import zio.test.{ Sized, TestFailure, TestSuccess, ZSpec }

trait TestCases {
  def specName: String
  def scenarios: TestScenarios

  def suite: Suite = test.suite(specName)(scenarios: _*)
}

object TestCases {
  type TestScenarios = List[ZSpec[Random with Sized, Throwable, String, Unit]]
  type Suite         = test.Spec[Random with Sized, TestFailure[Throwable], String, TestSuccess[Unit]]
}
