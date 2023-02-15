package zio.pulsar

import org.apache.pulsar.client.api.{ Consumer as JConsumer, Message, MessageId, PulsarClientException }
import zio.{ IO, ZIO }

import java.util.concurrent.TimeUnit
//import zio.blocking._
import zio.stream._
import scala.jdk.CollectionConverters._

final class Consumer[M](val consumer: JConsumer[M]):

  def acknowledge(messageId: MessageId): IO[PulsarClientException, Unit] =
    ZIO.attempt(consumer.acknowledge(messageId)).refineToOrDie[PulsarClientException]

  def acknowledge[T](message: Message[T]): IO[PulsarClientException, Unit] =
    ZIO.attempt(consumer.acknowledge(message)).refineToOrDie[PulsarClientException]

  def acknowledge(messages: Seq[MessageId]): IO[PulsarClientException, Unit] =
    ZIO.attempt(consumer.acknowledge(messages.asJava)).refineToOrDie[PulsarClientException]

  def negativeAcknowledge(messageId: MessageId): IO[PulsarClientException, Unit] =
    ZIO.attempt(consumer.negativeAcknowledge(messageId)).refineToOrDie[PulsarClientException]

  val receive: IO[PulsarClientException, Message[M]] =
    ZIO.attempt(consumer.receive).refineToOrDie[PulsarClientException]

  def receive(timeout: Int, unit: TimeUnit): IO[PulsarClientException, Message[M]] =
    ZIO.attempt(consumer.receive(timeout, unit)).refineToOrDie[PulsarClientException]

  val receiveAsync: IO[PulsarClientException, Message[M]] =
    ZIO.fromCompletionStage(consumer.receiveAsync).refineToOrDie[PulsarClientException]

  val receiveStream: Stream[PulsarClientException, Message[M]] =
    ZStream.repeatZIO(ZIO.attemptBlocking(consumer.receive).refineToOrDie[PulsarClientException])
