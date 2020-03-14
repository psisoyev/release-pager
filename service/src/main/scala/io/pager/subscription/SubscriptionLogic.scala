package io.pager.subscription

import io.pager.client.telegram.ChatId
import io.pager.logging.Logger
import io.pager.subscription.Repository.{ Name, Version }
import zio.{ Has, RIO, Task, ZIO, ZLayer }

object SubscriptionLogic {
  type SubscriptionLogic = Has[Service]

  trait Service {
    def subscribe(chatId: ChatId, name: Name): Task[Unit]
    def unsubscribe(chatId: ChatId, name: Name): Task[Unit]

    def listSubscriptions(chatId: ChatId): Task[Set[Name]]
    def listRepositories: Task[Map[Name, Option[Version]]]
    def listSubscribers(name: Name): Task[Set[ChatId]]

    def updateVersions(updatedVersions: Map[Name, Version]): Task[Unit]
  }

  final case class Live(
    logger: Logger.Service,
    chatStorage: ChatStorage.Service,
    repositoryVersionStorage: RepositoryVersionStorage.Service
  ) extends Service {
    override def subscribe(chatId: ChatId, name: Name): Task[Unit] =
      logger.info(s"$chatId subscribed to $name") *>
        chatStorage.subscribe(chatId, name) *>
        repositoryVersionStorage.addRepository(name)

    override def unsubscribe(chatId: ChatId, name: Name): Task[Unit] =
      logger.info(s"Chat $chatId unsubscribed from $name") *>
        chatStorage.unsubscribe(chatId, name)

    override def listSubscriptions(chatId: ChatId): Task[Set[Name]] =
      logger.info(s"Chat $chatId requested subscriptions") *>
        chatStorage.listSubscriptions(chatId)

    override def listRepositories: Task[Map[Name, Option[Version]]] =
      logger.info(s"Listing repositories") *>
        repositoryVersionStorage.listRepositories

    override def listSubscribers(name: Name): Task[Set[ChatId]] =
      logger.info(s"Listing repository ${name.value} subscribers") *>
        chatStorage.listSubscribers(name)

    override def updateVersions(updatedVersions: Map[Name, Version]): Task[Unit] =
      ZIO
        .foreach(updatedVersions) {
          case (name, version) =>
            logger.info(s"Updating repository ${name.value} version to $version") *>
              repositoryVersionStorage.updateVersion(name, version)
        }
        .unit
  }

  type LiveDeps = Has[Logger.Service] with Has[ChatStorage.Service] with Has[RepositoryVersionStorage.Service]
  val live: ZLayer[LiveDeps, Nothing, Has[Service]] =
    ZLayer.fromServices[Logger.Service, ChatStorage.Service, RepositoryVersionStorage.Service, Service] {
      (logger: Logger.Service, chatStorage: ChatStorage.Service, repositoryVersionStorage: RepositoryVersionStorage.Service) =>
        Live(logger, chatStorage, repositoryVersionStorage)
    }

  def subscribe(chatId: ChatId, name: Name): RIO[SubscriptionLogic, Unit]   = ZIO.accessM[SubscriptionLogic](_.get.subscribe(chatId, name))
  def unsubscribe(chatId: ChatId, name: Name): RIO[SubscriptionLogic, Unit] = ZIO.accessM[SubscriptionLogic](_.get.unsubscribe(chatId, name))

  def listSubscriptions(chatId: ChatId): RIO[SubscriptionLogic, Set[Name]] = ZIO.accessM[SubscriptionLogic](_.get.listSubscriptions(chatId))
  def listRepositories: RIO[SubscriptionLogic, Map[Name, Option[Version]]] = ZIO.accessM[SubscriptionLogic](_.get.listRepositories)
  def listSubscribers(name: Name): RIO[SubscriptionLogic, Set[ChatId]]     = ZIO.accessM[SubscriptionLogic](_.get.listSubscribers(name))

  def updateVersions(updatedVersions: Map[Name, Version]): RIO[SubscriptionLogic, Unit] =
    ZIO.accessM[SubscriptionLogic](_.get.updateVersions(updatedVersions))
}
