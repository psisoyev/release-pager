package io.pager

import io.pager.client.telegram.ChatId
import io.pager.subscription.Repository.Name
import zio.random.Random
import zio.test.Gen._
import zio.test.{ Gen, Sized }

object Generators {
  val repositoryName: Gen[Random with Sized, Name]      = anyString.map(Name)
  val chatId: Gen[Random with Sized, ChatId]            = anyLong.map(ChatId)
  val chatIds: Gen[Random with Sized, (ChatId, ChatId)] = chatId <*> chatId
}
