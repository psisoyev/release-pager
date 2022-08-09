package io.pager

import io.pager.client.telegram.ChatId
import io.pager.subscription.Repository.Name
import zio.Random
import zio.test.{ Gen, Sized }

object Generators {
  val repositoryName: Gen[Random with Sized, Name]      = Gen.string.map(Name)
  val chatId: Gen[Random with Sized, ChatId]            = Gen.long.map(ChatId)
  val chatIds: Gen[Random with Sized, (ChatId, ChatId)] = chatId <*> chatId
}
