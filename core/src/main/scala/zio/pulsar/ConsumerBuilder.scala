package zio.pulsar

import org.apache.pulsar.client.api.{ 
  BatchReceivePolicy, 
  ConsumerBuilder => JConsumerBuilder,
  DeadLetterPolicy, 
  KeySharedPolicy,
  PulsarClient => JPulsarClient,
  PulsarClientException,
  RegexSubscriptionMode,
  SubscriptionInitialPosition,
}
import zio.{ ZIO, ZManaged }
import zio.duration.Duration

case class Subscription[K <: SubscriptionKind](
  name: String, 
  `type`: SubscriptionType[K], 
  initialPosition: Option[SubscriptionInitialPosition] = None, 
  //properties: SubscriptionProperties
) { self =>
  
  def withInitialPosition(initialPosition: SubscriptionInitialPosition): Subscription[K] =
    self.copy(initialPosition = Some(initialPosition))

}

object Subscription:
  def apply[K <: SubscriptionKind](name: String, `type`: SubscriptionType[K], initialPosition: SubscriptionInitialPosition): Subscription[K] =
    Subscription(name, `type`, Some(initialPosition))


sealed trait SubscriptionMode

object SubscriptionMode:
  sealed trait Topic extends SubscriptionMode
  sealed trait Regex extends SubscriptionMode

sealed trait SubscriptionKind

object SubscriptionKind:
  sealed trait SingleConsumerSubscription extends SubscriptionKind
  sealed trait SharedSubscription         extends SubscriptionKind

sealed trait SubscriptionType[K <: SubscriptionKind]: 
  self =>
    def asJava: org.apache.pulsar.client.api.SubscriptionType = 
      self match
        case _: SubscriptionType.Exclusive.type => org.apache.pulsar.client.api.SubscriptionType.Exclusive
        case _: SubscriptionType.Failover.type  => org.apache.pulsar.client.api.SubscriptionType.Failover
        case _: SubscriptionType.KeyShared      => org.apache.pulsar.client.api.SubscriptionType.Key_Shared
        case _: SubscriptionType.Shared.type    => org.apache.pulsar.client.api.SubscriptionType.Shared

object SubscriptionType:
  object Exclusive                                    extends SubscriptionType[SubscriptionKind.SingleConsumerSubscription]
  object Failover                                     extends SubscriptionType[SubscriptionKind.SingleConsumerSubscription]
  final case class KeyShared(policy: KeySharedPolicy) extends SubscriptionType[SubscriptionKind.SharedSubscription]
  object Shared                                       extends SubscriptionType[SubscriptionKind.SharedSubscription]

sealed trait ConfigPart

object ConfigPart:
  sealed trait Empty      extends ConfigPart
  sealed trait Subscribed extends ConfigPart
  sealed trait ToTopic    extends ConfigPart

  type ConfigComplete = Empty with Subscribed with ToTopic

final class ConsumerBuilder[S <: ConfigPart, K <: SubscriptionKind, M <: SubscriptionMode](builder: JConsumerBuilder[Array[Byte]]):
  self =>

    import ConfigPart._
    import SubscriptionKind._
    import SubscriptionMode._
    import RegexSubscriptionMode._

    def withName(name: String): ConsumerBuilder[S, K, M] =
      new ConsumerBuilder(builder.consumerName(name))

    def withPattern(pattern: String): ConsumerBuilder[S with ToTopic, K, Regex] =
      new ConsumerBuilder(builder.topicsPattern(pattern))

    def withPatternAutoDiscoveryPeriod(minutes: Int)(implicit ev: M =:= Regex): ConsumerBuilder[S, K, M] =
      new ConsumerBuilder(builder.patternAutoDiscoveryPeriod(minutes))

    def withReadCompacted(implicit ev: K =:= SingleConsumerSubscription): ConsumerBuilder[S, K, M] =
      new ConsumerBuilder(builder.readCompacted(true))

    def withSubscription[K1 <: SubscriptionKind](subscription: Subscription[K1]): ConsumerBuilder[S with Subscribed, K1, M] =
      new ConsumerBuilder(
        subscription.initialPosition
          .fold(builder)(p => builder.subscriptionInitialPosition(p))
          .subscriptionType(subscription.`type`.asJava)
          .subscriptionName(subscription.name)
      )

    def withSubscriptionTopicsMode(mode: RegexSubscriptionMode)(implicit ev: M =:= Regex): ConsumerBuilder[S, K, M] =
      new ConsumerBuilder(builder.subscriptionTopicsMode(mode))

    def withTopic(topic: String): ConsumerBuilder[S with ToTopic, K, Topic] =
      new ConsumerBuilder(builder.topic(topic))

    def build(implicit ev: S =:= ConfigComplete): ZManaged[PulsarClient, PulsarClientException, Consumer] =
      val consumer = ZIO.effect(new Consumer(builder.subscribe)).refineToOrDie[PulsarClientException]
      ZManaged.make(consumer)(p => ZIO.effect(p.consumer.close).orDie)

object ConsumerBuilder:
  def apply(client: JPulsarClient): ConsumerBuilder[ConfigPart.Empty, Nothing, Nothing] = new ConsumerBuilder(client.newConsumer)

  val make: ZIO[PulsarClient, PulsarClientException, ConsumerBuilder[ConfigPart.Empty, Nothing, Nothing]] = 
    ZIO.accessM[PulsarClient](_.get.client).map(c => new ConsumerBuilder(c.newConsumer))

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