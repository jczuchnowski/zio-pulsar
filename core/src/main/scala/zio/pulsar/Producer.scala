package zio.pulsar

import org.apache.pulsar.client.api.{ MessageId, Producer => JProducer, PulsarClientException, Schema }
import zio.{ Has, IO, ZIO, ZManaged }
import zio.stream.{ Sink, ZSink }

final class Producer[M] private (val producer: JProducer[M]):

  def send(message: M): IO[PulsarClientException, MessageId] =
    ZIO.effect(producer.send(message)).refineToOrDie[PulsarClientException]

  def sendAsync(message: M): IO[PulsarClientException, MessageId] =
    ZIO.fromCompletionStage(producer.sendAsync(message)).refineToOrDie[PulsarClientException]

  def asSink: Sink[PulsarClientException, M, M, Unit] = ZSink.foreach(m => send(m))

  def asSinkAsync: Sink[PulsarClientException, M, M, Unit] = ZSink.foreach(m => sendAsync(m))

object Producer:

  def make(topic: String): ZManaged[Has[PulsarClient], PulsarClientException, Producer[Array[Byte]]] =
    val producer = PulsarClient.make.flatMap { client =>
      val builder = client.newProducer.topic(topic)
      ZIO.effect(new Producer(builder.create)).refineToOrDie[PulsarClientException]
    }
    ZManaged.make(producer)(p => ZIO.effect(p.producer.close).orDie)

  def make[M](topic: String, schema: Schema[M]): ZManaged[Has[PulsarClient], PulsarClientException, Producer[M]] =
    val producer = PulsarClient.make.flatMap { client =>
      val builder = client.newProducer(schema).topic(topic)
      ZIO.effect(new Producer(builder.create)).refineToOrDie[PulsarClientException]
    }
    ZManaged.make(producer)(p => ZIO.effect(p.producer.close).orDie)

