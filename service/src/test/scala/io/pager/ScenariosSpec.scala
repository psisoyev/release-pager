//package io.pager
//
//import canoe.api.{Bot, Scenario, TelegramClient}
//import canoe.methods.Method
//import canoe.models.messages.TextMessage
//import cats.Id
//import fs2.Stream
//import io.pager.Scenarios.{greetings, help}
//import org.scalatest.{Matchers, WordSpec}
//import fs2.Stream.Compiler._
//import zio._
//import zio.clock.Clock
//import zio.console._
//import zio.interop.catz._
//import canoe.syntax._
//
//import canoe.models.PrivateChat
//import canoe.models.messages.TextMessage
//import canoe.syntax._
//
//class ScenariosSpec extends WordSpec with Matchers {
//  "qwewqe" should {
//    "qwewqe" in new Scope {
//
//
//      val scenario: Scenario[IO, TextMessage] = Scenario.start(command("fire"))
//      val input = Stream.empty
//
//      assert(input.through(scenario.pipe).toList().isEmpty)
//
//
//    }
//  }
//
//  trait Scope {
//    implicit val client: TelegramClient[Id] = new TelegramClient[Id] {
//      override def execute[Req, Res](request: Req)(implicit M: Method[Req, Res]): Id[Res] = {
//        println("OLOLO")
//
//        ???
//      }
//    }
//  }
//}
