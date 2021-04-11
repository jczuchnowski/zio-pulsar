package zio.pulsar

import org.apache.pulsar.client.api.{
  Message => JMessage,
  MessageId,
  Consumer => JConsumer,
  PulsarClientException,
}
import zio.{ IO, ZIO }
import zio.blocking._
import zio.pulsar.codec.Decoder
import zio.stream._

final class Consumer[M](val consumer: JConsumer[Array[Byte]])(using decoder: Decoder[M]):

  def acknowledge(messageId: MessageId): IO[PulsarClientException, Unit] =
    ZIO.effect(consumer.acknowledge(messageId)).refineToOrDie[PulsarClientException]

  def negativeAcknowledge(messageId: MessageId): IO[PulsarClientException, Unit] =
    ZIO.effect(consumer.negativeAcknowledge(messageId)).refineToOrDie[PulsarClientException]

  val receive: IO[PulsarClientException, Message[M]] =
    ZIO.fromCompletionStage(consumer.receiveAsync).map(Message.from).refineToOrDie[PulsarClientException]

  val receiveStream: ZStream[Blocking, PulsarClientException, Message[M]] = 
    ZStream.repeatEffect(effectBlocking(Message.from(consumer.receive)).refineToOrDie[PulsarClientException])
