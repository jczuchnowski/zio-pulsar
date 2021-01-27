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
//import zio.pulsar.SubscriptionProperties._

//import scala.jdk.CollectionConverters._

final class Consumer(val consumer: JConsumer[Array[Byte]]) {

  def acknowledge(messageId: MessageId): IO[PulsarClientException, Unit] =
    ZIO.effect(consumer.acknowledge(messageId)).refineToOrDie[PulsarClientException]

  val receive: IO[PulsarClientException, Message[Array[Byte]]] =
    ZIO.fromCompletionStage(consumer.receiveAsync).refineToOrDie[PulsarClientException]

  val receiveStream: ZStream[Blocking, PulsarClientException, Message[Array[Byte]]] = 
    ZStream.repeatEffect(effectBlocking(consumer.receive).refineToOrDie[PulsarClientException])
}

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
