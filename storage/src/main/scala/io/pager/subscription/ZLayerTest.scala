package io.pager.subscription
import zio.ZLayer.NoDeps
import zio.{ Has, UIO, ZIO, ZLayer }

object ZLayerTest extends zio.App {
  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] = {

//    val env: ZLayer[Has[BookStorage.Service] with Any, RuntimeException, Has[BookLibrary.Service] with Nothing] = BookLibrary.live ++ BookStorage.live
//    val env2: ZLayer[Has[BookStorage.Service], RuntimeException, Nothing] = BookLibrary.live >>> BookStorage.live
    val env3: NoDeps[Nothing, Has[BookLibrary.Service]] = BookStorage.live >>> BookLibrary.live
    BookLibrary
      .findBook(1)
      .as(0)
      .provideLayer(env3)
  }

  type BookStorage = Has[BookStorage.Service]
  object BookStorage extends Serializable {
    trait Service extends Serializable {
      def findBook(id: Int): UIO[Option[String]]
    }

    val live: NoDeps[Nothing, Has[Service]] = ZLayer.fromEffect(sys.error("NOOO"))

    def findBook(id: Int): ZIO[Service, Nothing, Option[String]] = ZIO.accessM[Service](_.findBook(id))
  }

  type BookLibrary = Has[BookLibrary.Service]
  object BookLibrary extends Serializable {
    trait Service extends Serializable {
      def findBook(id: Int): UIO[Option[String]]
    }

    val live: ZLayer[Has[BookStorage.Service], Nothing, Has[Service]] = ZLayer.fromService { storage: BookStorage.Service =>
      new Service {
        override def findBook(id: Int): UIO[Option[String]] = storage.findBook(id)
      }
    }

    def findBook(id: Int): ZIO[BookLibrary, Nothing, Option[String]] = ZIO.accessM[BookLibrary](_.get.findBook(id))
  }

}
