package zio.pulsar

import org.apache.pulsar.client.api.{ MessageId, Producer => JProducer, PulsarClientException }
import zio.{ Has, IO, ZIO, ZManaged }
import zio.pulsar.codec.Encoder
import zio.stream.{ Sink, ZSink }

final class Producer[M] private (val producer: JProducer[Array[Byte]])(using encoder: Encoder[M]):

  def send(message: M): IO[PulsarClientException, MessageId] =
    ZIO.effect(producer.send(encoder.encode(message))).refineToOrDie[PulsarClientException]

  def sendAsync(message: M): IO[PulsarClientException, MessageId] =
    ZIO.fromCompletionStage(producer.sendAsync(encoder.encode(message))).refineToOrDie[PulsarClientException]

  def asSink: Sink[PulsarClientException, M, M, Unit] = ZSink.foreach(m => send(m))

  def asSinkAsync: Sink[PulsarClientException, M, M, Unit] = ZSink.foreach(m => sendAsync(m))

object Producer:

  def make[M](topic: String)(using encoder: Encoder[M]): ZManaged[Has[PulsarClient], PulsarClientException, Producer[M]] =
    val producer = PulsarClient.make.flatMap { client =>
      val builder = client.newProducer.topic(topic)
      ZIO.effect(new Producer(builder.create)).refineToOrDie[PulsarClientException]
    }
    ZManaged.make(producer)(p => ZIO.effect(p.producer.close).orDie)

