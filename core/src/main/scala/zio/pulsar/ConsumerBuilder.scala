package zio.pulsar

import java.util.concurrent.TimeUnit
import org.apache.pulsar.client.api.{
  BatchReceivePolicy as JBatchReceivePolicy,
  ConsumerBuilder as JConsumerBuilder,
  ConsumerEventListener as JConsumerEventListener,
  ConsumerInterceptor,
  DeadLetterPolicy as JDeadLetterPolicy,
  KeySharedPolicy,
  MessageListener,
  PulsarClient as JPulsarClient,
  PulsarClientException,
  RegexSubscriptionMode,
  Schema,
  SubscriptionInitialPosition,
  SubscriptionMode as JSubscriptionMode
}
import zio.pulsar.Properties.ObjectProperty
import zio.{ Duration, Scope, ZIO }

import scala.jdk.CollectionConverters.*
import java.util.regex.Pattern

case class Subscription[K <: SubscriptionKind](
  name: String,
  `type`: SubscriptionType[K],
  initialPosition: Option[SubscriptionInitialPosition] = None,
  properties: Map[String, String] = Map.empty
) { self =>

  def withInitialPosition(initialPosition: SubscriptionInitialPosition): Subscription[K] =
    self.copy(initialPosition = Some(initialPosition))

  def withProperties(properties: Map[String, String]): Subscription[K] =
    self.copy(properties = properties)
}

object Subscription:
  def apply[K <: SubscriptionKind](
    name: String,
    `type`: SubscriptionType[K],
    initialPosition: SubscriptionInitialPosition
  ): Subscription[K] =
    Subscription(name, `type`, Some(initialPosition))
end Subscription

sealed trait SubscriptionMode

object SubscriptionMode:
  sealed trait Topic extends SubscriptionMode
  sealed trait Regex extends SubscriptionMode
end SubscriptionMode

sealed trait SubscriptionKind

object SubscriptionKind:
  sealed trait SingleConsumerSubscription extends SubscriptionKind
  sealed trait SharedSubscription         extends SubscriptionKind
end SubscriptionKind

sealed trait SubscriptionType[K <: SubscriptionKind]:
  self =>
  def asJava: org.apache.pulsar.client.api.SubscriptionType =
    self match
      case _: SubscriptionType.Exclusive.type => org.apache.pulsar.client.api.SubscriptionType.Exclusive
      case _: SubscriptionType.Failover.type  => org.apache.pulsar.client.api.SubscriptionType.Failover
      case _: SubscriptionType.KeyShared      => org.apache.pulsar.client.api.SubscriptionType.Key_Shared
      case _: SubscriptionType.Shared.type    => org.apache.pulsar.client.api.SubscriptionType.Shared
end SubscriptionType

object SubscriptionType:
  object Exclusive                                    extends SubscriptionType[SubscriptionKind.SingleConsumerSubscription]
  object Failover                                     extends SubscriptionType[SubscriptionKind.SingleConsumerSubscription]
  final case class KeyShared(policy: KeySharedPolicy) extends SubscriptionType[SubscriptionKind.SharedSubscription]
  object Shared                                       extends SubscriptionType[SubscriptionKind.SharedSubscription]
end SubscriptionType

sealed trait ConsumerConfigPart

object ConsumerConfigPart:
  sealed trait Empty      extends ConsumerConfigPart
  sealed trait Subscribed extends ConsumerConfigPart
  sealed trait ToTopic    extends ConsumerConfigPart

  type ConfigComplete = Empty with Subscribed with ToTopic
end ConsumerConfigPart

final class ConsumerBuilder[T, S <: ConsumerConfigPart, K <: SubscriptionKind, M <: SubscriptionMode] private (
  builder: JConsumerBuilder[T]
):
  self =>

  import ConsumerConfigPart._
  import SubscriptionKind._
  import SubscriptionMode._
  import RegexSubscriptionMode._

  def loadConf(config: Properties): ConsumerBuilder[T, S, K, M] =
    new ConsumerBuilder(builder.loadConf(config.getProperties.asJava))

  def properties(properties: Properties): ConsumerBuilder[T, S, K, M] =
    new ConsumerBuilder(builder.properties(properties.getStringProperties.asJava))

  def messageListener(messageListener: MessageListener[T]): ConsumerBuilder[T, S, K, M] =
    new ConsumerBuilder(builder.messageListener(messageListener))

  def intercept(
    interceptor: ConsumerInterceptor[T],
    interceptors: ConsumerInterceptor[T]*
  ): ConsumerBuilder[T, S, K, M] =
    new ConsumerBuilder(builder.intercept(Seq(interceptor) ++ interceptors: _*))

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

  def maxTotalReceiverQueueSizeAcrossPartitions(
    maxTotalReceiverQueueSizeAcrossPartitions: Int
  ): ConsumerBuilder[T, S, K, M] =
    new ConsumerBuilder(builder.maxTotalReceiverQueueSizeAcrossPartitions(maxTotalReceiverQueueSizeAcrossPartitions))

  def maxPendingChunkedMessage(max: Int): ConsumerBuilder[T, S, K, M] =
    new ConsumerBuilder(builder.maxPendingChunkedMessage(max))

  def consumerEventListener(consumerEventListener: JConsumerEventListener): ConsumerBuilder[T, S, K, M] =
    new ConsumerBuilder(builder.consumerEventListener(consumerEventListener))

  def consumerName(name: String): ConsumerBuilder[T, S, K, M] =
    new ConsumerBuilder(builder.consumerName(name))

  def negativeAckRedeliveryDelay(redeliveryDelay: Long, unit: TimeUnit): ConsumerBuilder[T, S, K, M] =
    new ConsumerBuilder(builder.negativeAckRedeliveryDelay(redeliveryDelay, unit))

  def pattern(pattern: String): ConsumerBuilder[T, S with ToTopic, K, Regex] =
    new ConsumerBuilder(builder.topicsPattern(pattern))

  def pattern(pattern: Pattern): ConsumerBuilder[T, S with ToTopic, K, Regex] =
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

  def subscription[K1 <: SubscriptionKind](
    subscription: Subscription[K1]
  ): ConsumerBuilder[T, S with Subscribed, K1, M] =
    new ConsumerBuilder(
      subscription.initialPosition
        .fold(builder)(p => builder.subscriptionInitialPosition(p))
        .subscriptionType(subscription.`type`.asJava)
        .subscriptionName(subscription.name)
        .subscriptionProperties(subscription.properties.asJava)
    )

  def subscriptionTopicsMode(mode: RegexSubscriptionMode)(implicit ev: M =:= Regex): ConsumerBuilder[T, S, K, M] =
    new ConsumerBuilder(builder.subscriptionTopicsMode(mode))

  def subscriptionMode(subscriptionMode: JSubscriptionMode)(implicit ev: M =:= Topic): ConsumerBuilder[T, S, K, M] =
    new ConsumerBuilder(builder.subscriptionMode(subscriptionMode))

  def topic(topic: String): ConsumerBuilder[T, S with ToTopic, K, Topic] =
    new ConsumerBuilder(builder.topic(topic))

  def build(implicit ev: S =:= ConfigComplete): ZIO[PulsarClient with Scope, PulsarClientException, Consumer[T]] =
    val consumer = ZIO.attempt(new Consumer(builder.subscribe)).refineToOrDie[PulsarClientException]
    ZIO.acquireRelease(consumer)(p => ZIO.attempt(p.consumer.close()).orDie)

end ConsumerBuilder

object ConsumerBuilder:

  lazy val make: ZIO[PulsarClient, PulsarClientException, ConsumerBuilder[Array[
    Byte
  ], ConsumerConfigPart.Empty, Nothing, Nothing]] =
    ZIO.environmentWithZIO[PulsarClient](_.get.client).map(c => new ConsumerBuilder(c.newConsumer))

  def make[M](
    schema: Schema[M]
  ): ZIO[PulsarClient, PulsarClientException, ConsumerBuilder[M, ConsumerConfigPart.Empty, Nothing, Nothing]] =
    ZIO.environmentWithZIO[PulsarClient](_.get.client).map(c => new ConsumerBuilder(c.newConsumer(schema)))
