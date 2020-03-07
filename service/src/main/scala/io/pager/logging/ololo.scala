package io.pager.logging

import zio._
import zio.console.Console

object XXX {
  type Logging = Has[Logging.Service]
  object Logging {
    trait Service {
      def logLine(line: String): UIO[Unit]
    }

    val live: ZLayer[Console, Nothing, Logging] =
      ZLayer.fromService { console =>
        new Logging.Service {
          override def logLine(line: String): UIO[Unit] =
            console.putStrLn(line)
        }
      }
  }

  type Authorization = Has[Authorization.Service]
  object Authorization {
    trait Service {
      def key: Long
    }

    val live: ZLayer.NoDeps[Nothing, Authorization] =
      ZLayer.succeed {
        new Authorization.Service {
          val key = 123L
        }
      }
  }

  type Database[K, V] = Has[Database.Service[K, V]]
  object Database {
    trait Service[K, V] {
      def get(key: K): UIO[Option[V]]
      def set(key: K, value: V): UIO[Unit]
    }

    final case class InMemory[K, V](
      state: Ref[Map[K, V]],
      logging: Logging.Service,
      authorization: Authorization.Service
    ) extends Database.Service[K, V] {
      override def get(key: K): UIO[Option[V]]      = state.get.map(_.get(key))
      override def set(key: K, value: V): UIO[Unit] = state.update(_.updated(key, value)).unit
    }

    def inMemory[K, V](implicit tag: Tagged[Database.Service[K, V]]): ZLayer[Logging with Authorization, Nothing, Database[K, V]] =
      ZLayer.fromServicesM[Logging.Service, Authorization.Service, Any, Nothing, Database.Service[K, V]] {
        (logging: Logging.Service, authorization: Authorization.Service) =>
          Ref.make(Map.empty[K, V]).map { state =>
            InMemory(state, logging, authorization)
          }
      }
  }
}
