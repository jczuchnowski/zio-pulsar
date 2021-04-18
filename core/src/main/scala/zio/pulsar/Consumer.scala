package zio.pulsar

import org.apache.pulsar.client.api.{
  Message,
  MessageId,
  Consumer => JConsumer,
  PulsarClientException,
}
import zio.{ IO, ZIO }
import zio.blocking._
import zio.stream._

final class Consumer[M](val consumer: JConsumer[M]):

  def acknowledge(messageId: MessageId): IO[PulsarClientException, Unit] =
    ZIO.effect(consumer.acknowledge(messageId)).refineToOrDie[PulsarClientException]

  def negativeAcknowledge(messageId: MessageId): IO[PulsarClientException, Unit] =
    ZIO.effect(consumer.negativeAcknowledge(messageId)).refineToOrDie[PulsarClientException]

  val receive: IO[PulsarClientException, Message[M]] =
    ZIO.effect(consumer.receive).refineToOrDie[PulsarClientException]

  val receiveAsync: IO[PulsarClientException, Message[M]] =
    ZIO.fromCompletionStage(consumer.receiveAsync).refineToOrDie[PulsarClientException]

  val receiveStream: ZStream[Blocking, PulsarClientException, Message[M]] = 
    ZStream.repeatEffect(effectBlocking(consumer.receive).refineToOrDie[PulsarClientException])
