package zio.pulsar

import org.apache.pulsar.client.api.{ 
  BatchReceivePolicy, 
  ConsumerBuilder => JConsumerBuilder,
  DeadLetterPolicy, 
  KeySharedPolicy,
  PulsarClientException,
  SubscriptionInitialPosition,
}
import zio.{ ZIO, ZManaged }
import zio.duration.Duration

case class Subscription[K <: SubscriptionKind](
  name: String, 
  `type`: SubscriptionType[K], 
  initialPosition: Option[SubscriptionInitialPosition] = None, 
  //properties: SubscriptionProperties
):
  self =>
  
  def withInitialPosition(initialPosition: SubscriptionInitialPosition): Subscription[K] =
    self.copy(initialPosition = Some(initialPosition))

object Subscription:
  def apply[K <: SubscriptionKind](name: String, `type`: SubscriptionType[K], initialPosition: SubscriptionInitialPosition): Subscription[K] =
    Subscription(name, `type`, Some(initialPosition))

enum SubscriptionKind:
  case SingleConsumerSubscription
  case SharedSubscription

// sealed trait SubscriptionType[K <: SubscriptionKind] { self =>
//   def asJava: org.apache.pulsar.client.api.SubscriptionType = 
//     self match
//       case _: SubscriptionType.Exclusive.type => org.apache.pulsar.client.api.SubscriptionType.Exclusive
//       case _: SubscriptionType.Failover.type  => org.apache.pulsar.client.api.SubscriptionType.Failover
//       case _: SubscriptionType.KeyShared      => org.apache.pulsar.client.api.SubscriptionType.Key_Shared
//       case _: SubscriptionType.Shared.type    => org.apache.pulsar.client.api.SubscriptionType.Shared
// }

enum SubscriptionType[K <: SubscriptionKind]:
  self =>
  case Exclusive                          extends SubscriptionType[SubscriptionKind.SingleConsumerSubscription.type]
  case Failover                           extends SubscriptionType[SubscriptionKind.SingleConsumerSubscription.type]
  case KeyShared(policy: KeySharedPolicy) extends SubscriptionType[SubscriptionKind.SharedSubscription.type]
  case Shared                             extends SubscriptionType[SubscriptionKind.SharedSubscription.type]

  def asJava: org.apache.pulsar.client.api.SubscriptionType = 
    self match
      case _: SubscriptionType.Exclusive.type => org.apache.pulsar.client.api.SubscriptionType.Exclusive
      case _: SubscriptionType.Failover.type  => org.apache.pulsar.client.api.SubscriptionType.Failover
      case _: SubscriptionType.KeyShared      => org.apache.pulsar.client.api.SubscriptionType.Key_Shared
      case _: SubscriptionType.Shared.type    => org.apache.pulsar.client.api.SubscriptionType.Shared

enum ConfigPart:
  case Empty
  case Subscribed
  case ToTopic

type Full = ConfigPart.Empty.type with ConfigPart.Subscribed.type with ConfigPart.ToTopic.type

final class ConsumerBuilder[S <: ConfigPart, K <: SubscriptionKind](builder: JConsumerBuilder[Array[Byte]]):
  self =>

  import ConfigPart._
  import SubscriptionKind._

  def withReadCompacted(implicit ev: K =:= SharedSubscription.type): ConsumerBuilder[S, K] =
    new ConsumerBuilder(builder.readCompacted(true))

  def withSubscription[K1 <: SubscriptionKind](subscription: Subscription[K1]): ConsumerBuilder[S with Subscribed.type, K1] =
    new ConsumerBuilder(
      subscription.initialPosition
        .fold(builder)(p => builder.subscriptionInitialPosition(p))
        .subscriptionType(subscription.`type`.asJava)
        .subscriptionName(subscription.name)
    )

  def withTopic(topic: String): ConsumerBuilder[S with ToTopic.type, K] =
    new ConsumerBuilder(builder.topic(topic))

  def build(implicit ev: S =:= Full): ZManaged[PulsarClient, PulsarClientException, Consumer] =
    val consumer = ZIO.effect(new Consumer(builder.subscribe)).refineToOrDie[PulsarClientException]
    ZManaged.make(consumer)(p => ZIO.effect(p.consumer.close).orDie)

object ConsumerBuilder:
  def apply(builder: JConsumerBuilder[Array[Byte]]): ConsumerBuilder[ConfigPart.Empty.type, Nothing] = new ConsumerBuilder(builder)

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