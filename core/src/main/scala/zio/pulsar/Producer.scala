package zio.pulsar

import org.apache.pulsar.client.api.{ MessageId, Producer as JProducer, PulsarClientException, Schema }
import zio.{ IO, Scope, ZIO }
import zio.stream.{ Sink, ZSink }

final class Producer[M](val producer: JProducer[M]):

  def send(message: M): IO[PulsarClientException, MessageId] =
    ZIO.attempt(producer.send(message)).refineToOrDie[PulsarClientException]

  def sendAsync(message: M): IO[PulsarClientException, MessageId] =
    ZIO.fromCompletionStage(producer.sendAsync(message)).refineToOrDie[PulsarClientException]

  def asSink: Sink[PulsarClientException, M, M, Unit] = ZSink.foreach(m => send(m))

  def asSinkAsync: Sink[PulsarClientException, M, M, Unit] = ZSink.foreach(m => sendAsync(m))
end Producer

object Producer:

  def make(topic: String): ZIO[PulsarClient & Scope, PulsarClientException, Producer[Array[Byte]]] =
    val producer = PulsarClient.make.flatMap { client =>
      val builder = client.newProducer.topic(topic)
      ZIO.attempt(new Producer(builder.create)).refineToOrDie[PulsarClientException]
    }
    ZIO.acquireRelease(producer)(p => ZIO.attempt(p.producer.close()).orDie)

  def make[M](topic: String, schema: Schema[M]): ZIO[PulsarClient & Scope, PulsarClientException, Producer[M]] =
    val producer = PulsarClient.make.flatMap { client =>
      val builder = client.newProducer(schema).topic(topic)
      ZIO.attempt(new Producer(builder.create)).refineToOrDie[PulsarClientException]
    }
    ZIO.acquireRelease(producer)(p => ZIO.attempt(p.producer.close()).orDie)
