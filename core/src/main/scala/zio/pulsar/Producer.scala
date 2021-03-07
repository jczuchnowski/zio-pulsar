package zio.pulsar

import org.apache.pulsar.client.api.{ MessageId, Producer => JProducer, PulsarClientException }
import zio.{ IO, ZIO, ZManaged }
import zio.stream.ZSink

trait Encoder[M]:
  def encode(m: M): Array[Byte]

given stringEncoder: Encoder[String] with
  def encode(m: String) = m.getBytes

final class Producer[M] private (val producer: JProducer[Array[Byte]])(using encoder: Encoder[M]):

  def send(message: M): IO[PulsarClientException, MessageId] =
    ZIO.effect(producer.send(encoder.encode(message))).refineToOrDie[PulsarClientException]

  def asSink = ZSink.foreach(m => send(m))

object Producer:

  def make[M](topic: String)(using encoder: Encoder[M]): ZManaged[PulsarClient, PulsarClientException, Producer[M]] =
    val producer = PulsarClient.make.flatMap { client =>
      val builder = client.newProducer.topic(topic)
      ZIO.effect(new Producer(builder.create)).refineToOrDie[PulsarClientException]
    }
    ZManaged.make(producer)(p => ZIO.effect(p.producer.close).orDie)

