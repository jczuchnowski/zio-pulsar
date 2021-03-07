package zio.pulsar

import org.apache.pulsar.client.api.{
  Message => JMessage,
  MessageId,
  Consumer => JConsumer,
  PulsarClientException,
}
import zio.{ IO, ZIO }
import zio.blocking._
import zio.stream._
//import zio.pulsar.SubscriptionProperties._

//import scala.jdk.CollectionConverters._

trait Decoder[M]:
  def decode(m: Array[Byte]): M

given stringDecoder: Decoder[String] with
  def decode(m: Array[Byte]) = new String(m)

final case class Message[M](id: MessageId, value: M)

object Message:
  def from[M](m: JMessage[Array[Byte]])(using decoder: Decoder[M]): Message[M] =
    Message(m.getMessageId, decoder.decode(m.getValue))

final class Consumer[M](val consumer: JConsumer[Array[Byte]])(using decoder: Decoder[M]):

  def acknowledge(messageId: MessageId): IO[PulsarClientException, Unit] =
    ZIO.effect(consumer.acknowledge(messageId)).refineToOrDie[PulsarClientException]

  def negativeAcknowledge(messageId: MessageId): IO[PulsarClientException, Unit] =
    ZIO.effect(consumer.negativeAcknowledge(messageId)).refineToOrDie[PulsarClientException]

  val receive: IO[PulsarClientException, Message[M]] =
    ZIO.fromCompletionStage(consumer.receiveAsync).map(Message.from).refineToOrDie[PulsarClientException]

  val receiveStream: ZStream[Blocking, PulsarClientException, Message[M]] = 
    ZStream.repeatEffect(effectBlocking(Message.from(consumer.receive)).refineToOrDie[PulsarClientException])

// object Consumer {

//   private def subscriptionType(t: SubscriptionType, builder: ConsumerBuilder[Array[Byte]]): ConsumerBuilder[Array[Byte]] =
//     t match {
//       case SubscriptionType.Exclusive(r) => 
//         builder
//           .subscriptionType(JSubscriptionType.Exclusive)
//           .readCompacted(r)

//       case SubscriptionType.Failover(r)  => 
//         builder
//           .subscriptionType(JSubscriptionType.Failover)
//           .readCompacted(r)

//       case SubscriptionType.Shared       => 
//         builder
//           .subscriptionType(JSubscriptionType.Shared)
//           .readCompacted(false)

//       case SubscriptionType.KeyShared(p) => 
//         builder
//           .subscriptionType(JSubscriptionType.Key_Shared)
//           .readCompacted(false)
//           .keySharedPolicy(p)
//     }

//   private def consumerBuilder(client: JPulsarClient, subscription: Subscription) = {
//     val consumer = {
//       val cons = subscription.`type`.fold(client.newConsumer)(t => subscriptionType(t, client.newConsumer))
//         .subscriptionName(subscription.name)

//       subscription.initialPosition.fold(cons)(p => cons.subscriptionInitialPosition(p))
//     }

//     subscription.properties match {
//       case TopicSubscriptionProperties(topics, mode) =>
//         val cons = consumer
//           .topics(topics.asJava)
//         mode.fold(cons)(m => cons.subscriptionMode(m))
//       case PatternSubscriptionProperties(pattern, mode, period) =>
//         val cons = consumer
//           .topicsPattern(pattern)
//         val cons_ = mode.fold(cons)(m => cons.subscriptionTopicsMode(m))
//         period.fold(cons_)(p => cons_.patternAutoDiscoveryPeriod(p))
          
//     }
//   }

//   def subscribe(subscription: Subscription): ZManaged[PulsarClient, PulsarClientException, Consumer] = {
//     val consumer = PulsarClient.client.map { client =>
//       val builder = consumerBuilder(client, subscription)
//       new Consumer(builder.subscribe)
//     }
//     ZManaged.make(consumer)(p => ZIO.effect(p.consumer.close).orDie)
//   }

// }
