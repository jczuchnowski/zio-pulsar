package zio.pulsar

import org.apache.pulsar.client.api.{ 
  BatchReceivePolicy, 
  ConsumerBuilder => JConsumerBuilder,
  DeadLetterPolicy, 
  KeySharedPolicy,
  PulsarClientException
}
import zio.{ ZIO, ZManaged }
import zio.duration.Duration

case class Subscription[K <: SubscriptionKind](
  name: String, 
  `type`: SubscriptionType[K], 
  //initialPosition: Option[SubscriptionInitialPosition] = None, 
  //properties: SubscriptionProperties
)

sealed trait SubscriptionKind

object SubscriptionKind {
  sealed trait SingleConsumerSubscription extends SubscriptionKind
  sealed trait SharedSubscription         extends SubscriptionKind
}

sealed trait SubscriptionType[K <: SubscriptionKind] { self =>
  def asJava: org.apache.pulsar.client.api.SubscriptionType = 
    self match {
      case _: SubscriptionType.Exclusive.type => org.apache.pulsar.client.api.SubscriptionType.Exclusive
      case _: SubscriptionType.Failover.type  => org.apache.pulsar.client.api.SubscriptionType.Failover
      case _: SubscriptionType.KeyShared      => org.apache.pulsar.client.api.SubscriptionType.Key_Shared
      case _: SubscriptionType.Shared.type    => org.apache.pulsar.client.api.SubscriptionType.Shared
    }
}

object SubscriptionType {
  object Exclusive                                    extends SubscriptionType[SubscriptionKind.SingleConsumerSubscription]
  object Failover                                     extends SubscriptionType[SubscriptionKind.SingleConsumerSubscription]
  final case class KeyShared(policy: KeySharedPolicy) extends SubscriptionType[SubscriptionKind.SharedSubscription]
  object Shared                                       extends SubscriptionType[SubscriptionKind.SharedSubscription]
}

sealed trait ConfigPart

object ConfigPart {
  sealed trait Empty      extends ConfigPart
  sealed trait Subscribed extends ConfigPart
  sealed trait ToTopic    extends ConfigPart

  type Full = Empty with Subscribed with ToTopic
}

final class ConsumerBuilder[S <: ConfigPart, K <: SubscriptionKind](builder: JConsumerBuilder[Array[Byte]]) { self =>

  import ConfigPart._
  import SubscriptionKind._

  def withReadCompacted(implicit ev: K =:= SharedSubscription): ConsumerBuilder[S, K] =
    new ConsumerBuilder(builder.readCompacted(true))

  def withSubscription[K1 <: SubscriptionKind](subscription: Subscription[K1]): ConsumerBuilder[S with Subscribed, K1] =
    new ConsumerBuilder(builder.subscriptionType(subscription.`type`.asJava).subscriptionName(subscription.name))

  def withTopic(topic: String): ConsumerBuilder[S with ToTopic, K] =
    new ConsumerBuilder(builder.topic(topic))

  def build(implicit ev: S =:= Full): ZManaged[PulsarClient, PulsarClientException, Consumer] = {
    val consumer = ZIO.effect(new Consumer(builder.subscribe)).refineToOrDie[PulsarClientException]
    ZManaged.make(consumer)(p => ZIO.effect(p.consumer.close).orDie)
  }
}

object ConsumerBuilder {

  def apply(builder: JConsumerBuilder[Array[Byte]]): ConsumerBuilder[ConfigPart.Empty, Nothing] = new ConsumerBuilder(builder)

}

// trait SubscriptionProperties

// object SubscriptionProperties {
//   case class TopicSubscriptionProperties(topics: List[String], mode: Option[SubscriptionMode] = None) extends SubscriptionProperties
//   case class PatternSubscriptionProperties(topicsPattern: String, mode: Option[RegexSubscriptionMode] = None, patternAutoDiscoveryPeriod: Option[Int] = None)
//       extends SubscriptionProperties
// }

final case class ConsumerConfig(
  ackTimeout: Duration,
  ackTimeoutTickTime: Duration,
  negativeAckRedeliveryDelay: Duration,
  receiverQueueSize: Int,
  acknowledgmentGroupTime: Duration,
  replicateSubscriptionState: Boolean,
  maxTotalReceiverQueueSizeAcrossPartitions: Int,
  priorityLevel: Int,
  properties: Map[String, String],
  deadLetterPolicy: Option[DeadLetterPolicy],
  autoUpdatePartitions: Boolean,
  autoUpdatePartitionsInterval: Duration,
  batchReceivePolicy: Option[BatchReceivePolicy],
  enableRetry: Boolean,
  enableBatchIndexAcknowledgment: Boolean,
  maxPendingChuckedMessage: Int,
  autoAckOldestChunkedMessageOnQueueFull: Boolean,
  expireTimeOfIncompleteChunkedMessage: Duration
  //startMessageIdInclusive
  ///intercept
  //cryptoKeyReader
  //messageCrypto
  //cryptoFailureAction
  //consumerEventListener
)