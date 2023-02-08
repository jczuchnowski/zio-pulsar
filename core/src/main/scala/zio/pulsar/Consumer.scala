package zio.pulsar

import org.apache.pulsar.client.api.{ Consumer => JConsumer, Message, MessageId, PulsarClientException }
import zio.{ IO, ZIO }
//import zio.blocking._
import zio.stream._

final class Consumer[M](val consumer: JConsumer[M]):

  def acknowledge(messageId: MessageId): IO[PulsarClientException, Unit] =
    ZIO.attempt(consumer.acknowledge(messageId)).refineToOrDie[PulsarClientException]

  def acknowledge[T](message: Message[T]): IO[PulsarClientException, Unit] =
    ZIO.attempt(consumer.acknowledge(message)).refineToOrDie[PulsarClientException]

  def negativeAcknowledge(messageId: MessageId): IO[PulsarClientException, Unit] =
    ZIO.attempt(consumer.negativeAcknowledge(messageId)).refineToOrDie[PulsarClientException]

  val receive: IO[PulsarClientException, Message[M]] =
    ZIO.attempt(consumer.receive).refineToOrDie[PulsarClientException]

  val receiveAsync: IO[PulsarClientException, Message[M]] =
    ZIO.fromCompletionStage(consumer.receiveAsync).refineToOrDie[PulsarClientException]

  val receiveStream: Stream[PulsarClientException, Message[M]] =
    ZStream.repeatZIO(ZIO.attemptBlocking(consumer.receive).refineToOrDie[PulsarClientException])
