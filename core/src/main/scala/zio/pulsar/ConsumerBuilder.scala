package zio.pulsar

import java.util.concurrent.TimeUnit

import org.apache.pulsar.client.api.{ 
  BatchReceivePolicy => JBatchReceivePolicy, 
  ConsumerBuilder => JConsumerBuilder,
  ConsumerEventListener => JConsumerEventListener,
  DeadLetterPolicy => JDeadLetterPolicy, 
  KeySharedPolicy,
  PulsarClient => JPulsarClient,
  PulsarClientException,
  RegexSubscriptionMode,
  Schema,
  SubscriptionInitialPosition,
}
import zio.{ Duration, ZIO, ZManaged }

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

final class ConsumerBuilder[T, S <: ConfigPart, K <: SubscriptionKind, M <: SubscriptionMode](builder: JConsumerBuilder[T]):
  self =>

    import ConfigPart._
    import SubscriptionKind._
    import SubscriptionMode._
    import RegexSubscriptionMode._

    def acknowledgmentGroupTime(delay: Long, unit: TimeUnit): ConsumerBuilder[T, S, K, M] =
      new ConsumerBuilder(builder.acknowledgmentGroupTime(delay, unit))

    def ackTimeout(ackTimeout: Long, unit: TimeUnit): ConsumerBuilder[T, S, K, M] =
      new ConsumerBuilder(builder.ackTimeout(ackTimeout, unit))

    def ackTimeoutTickTime(tickTime: Long, unit: TimeUnit): ConsumerBuilder[T, S, K, M] =
      new ConsumerBuilder(builder.ackTimeoutTickTime(tickTime, unit))

    def autoAckOldestChunkedMessageOnQueueFull(autoAck: Boolean): ConsumerBuilder[T, S, K, M] =
      new ConsumerBuilder(builder.autoAckOldestChunkedMessageOnQueueFull(autoAck))

    def batchReceivePolicy(policy: JBatchReceivePolicy): ConsumerBuilder[T, S, K, M] =
      new ConsumerBuilder(builder.batchReceivePolicy(policy))
    
    def deadLetterPolicy(policy: JDeadLetterPolicy): ConsumerBuilder[T, S, K, M] =
      new ConsumerBuilder(builder.deadLetterPolicy(policy))

    def enableBatchIndexAcknowledgment(batchIndexAcknowledgment: Boolean): ConsumerBuilder[T, S, K, M] =
      new ConsumerBuilder(builder.enableBatchIndexAcknowledgment(batchIndexAcknowledgment))

    def enableRetry(retry: Boolean): ConsumerBuilder[T, S, K, M] =
      new ConsumerBuilder(builder.enableRetry(retry))

    def expireTimeOfIncompleteChunkedMessage(duration: Long, unit: TimeUnit): ConsumerBuilder[T, S, K, M] =
      new ConsumerBuilder(builder.expireTimeOfIncompleteChunkedMessage(duration, unit))

    def maxTotalReceiverQueueSizeAcrossPartitions(maxTotalReceiverQueueSizeAcrossPartitions: Int): ConsumerBuilder[T, S, K, M] =
      new ConsumerBuilder(builder.maxTotalReceiverQueueSizeAcrossPartitions(maxTotalReceiverQueueSizeAcrossPartitions))

    def maxPendingChuckedMessage(max: Int): ConsumerBuilder[T, S, K, M] =
      new ConsumerBuilder(builder.maxPendingChuckedMessage(max))

    def consumerEventListener(consumerEventListener: JConsumerEventListener): ConsumerBuilder[T, S, K, M] =
      new ConsumerBuilder(builder.consumerEventListener(consumerEventListener))

    def consumerName(name: String): ConsumerBuilder[T, S, K, M] =
      new ConsumerBuilder(builder.consumerName(name))

    def negativeAckRedeliveryDelay(redeliveryDelay: Long, unit: TimeUnit): ConsumerBuilder[T, S, K, M] =
      new ConsumerBuilder(builder.negativeAckRedeliveryDelay(redeliveryDelay, unit))

    def pattern(pattern: String): ConsumerBuilder[T, S with ToTopic, K, Regex] =
      new ConsumerBuilder(builder.topicsPattern(pattern))

    def patternAutoDiscoveryPeriod(interval: Int, unit: TimeUnit)(implicit ev: M =:= Regex): ConsumerBuilder[T, S, K, M] =
      new ConsumerBuilder(builder.patternAutoDiscoveryPeriod(interval, unit))

    def priorityLevel(level: Int): ConsumerBuilder[T, S, K, M] =
      new ConsumerBuilder(builder.priorityLevel(level))

    def readCompacted(implicit ev: K =:= SingleConsumerSubscription): ConsumerBuilder[T, S, K, M] =
      new ConsumerBuilder(builder.readCompacted(true))

    def receiverQueueSize(receiverQueueSize: Int): ConsumerBuilder[T, S, K, M] =
      new ConsumerBuilder(builder.receiverQueueSize(receiverQueueSize))

    def replicateSubscriptionState(replicateSubscriptionState: Boolean): ConsumerBuilder[T, S, K, M] =
      new ConsumerBuilder(builder.replicateSubscriptionState(replicateSubscriptionState))

    def subscription[K1 <: SubscriptionKind](subscription: Subscription[K1]): ConsumerBuilder[T, S with Subscribed, K1, M] =
      new ConsumerBuilder(
        subscription.initialPosition
          .fold(builder)(p => builder.subscriptionInitialPosition(p))
          .subscriptionType(subscription.`type`.asJava)
          .subscriptionName(subscription.name)
      )

    def subscriptionTopicsMode(mode: RegexSubscriptionMode)(implicit ev: M =:= Regex): ConsumerBuilder[T, S, K, M] =
      new ConsumerBuilder(builder.subscriptionTopicsMode(mode))

    def topic(topic: String): ConsumerBuilder[T, S with ToTopic, K, Topic] =
      new ConsumerBuilder(builder.topic(topic))

    def build(implicit ev: S =:= ConfigComplete): ZManaged[PulsarClient, PulsarClientException, Consumer[T]] =
      val consumer = ZIO.effect(new Consumer(builder.subscribe)).refineToOrDie[PulsarClientException]
      ZManaged.make(consumer)(p => ZIO.effect(p.consumer.close).orDie)

object ConsumerBuilder:

  val make: ZIO[PulsarClient, PulsarClientException, ConsumerBuilder[Array[Byte], ConfigPart.Empty, Nothing, Nothing]] = 
    ZIO.accessM[PulsarClient](_.get.client).map(c => new ConsumerBuilder(c.newConsumer))

  def make[M](schema: Schema[M]): ZIO[PulsarClient, PulsarClientException, ConsumerBuilder[M, ConfigPart.Empty, Nothing, Nothing]] = 
    ZIO.accessM[PulsarClient](_.get.client).map(c => new ConsumerBuilder(c.newConsumer(schema)))
