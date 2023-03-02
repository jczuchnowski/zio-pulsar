package zio.pulsar

import Properties.*
import org.apache.pulsar.client.api.{
  CompressionType,
  ConsumerCryptoFailureAction,
  DeadLetterPolicy,
  HashingScheme,
  MessageRoutingMode,
  ProducerCryptoFailureAction,
  RedeliveryBackoff,
  RegexSubscriptionMode,
  SubscriptionInitialPosition,
  SubscriptionType as JSubscriptionType
}

import java.util.regex.Pattern
import scala.collection.immutable.SortedMap
import scala.jdk.CollectionConverters.*

/**
 * @author
 *   梦境迷离
 * @version 1.0,2023/2/16
 */
private[pulsar] abstract class Properties private (
  private val propertyList: List[Property[_]] = Nil
):
  import Properties.*
  import Property.*
  import Consumer.*
  import Producer.*

  def getProperties: Map[String, String] = getConfig.map(f => f._1 -> f._2.toString)

  def getConfig: Map[String, Any] =
    this.propertyList
      .filter(_ != null)
      .foldLeft(List.empty[(String, Any)]) { (op, a) =>
        op.:: {
          val key = a._key.getOrElse(a.name)
          a match
            case topicNames(value) => key -> value.toSet
            case properties(value) => key -> value.asJava
            case _                 => key -> a.value
        }
      }
      .toMap
end Properties

object Properties:
  import Property.*

  private[pulsar] final case class ConsumerProperties(c: Consumer[_], cl: List[Consumer[_]]) extends Properties(c :: cl)
  private[pulsar] final case class ProducerProperties(p: Producer[_], pl: List[Producer[_]]) extends Properties(p :: pl)
  private[pulsar] final case class StringProperties(s: StringProperty, sl: List[StringProperty])
      extends Properties(s :: sl)

end Properties

sealed trait Property[+T]:
  self =>
  def name: String = self.getClass.getSimpleName

  def value: T

  def _key: Option[String] = None
end Property

object Property:
  final case class StringProperty(key: String, value: String) extends Property[String] {
    override def _key: Option[String] = Option(key)
  }

  sealed trait ListProperty[+T] extends Property[List[T]]:
    override def value: List[T]
  end ListProperty

  sealed trait MapProperty[K, V] extends Property[SortedMap[K, V]]:
    override def value: SortedMap[K, V]
  end MapProperty

  sealed trait Producer[+T] extends Property[T]

  sealed trait Consumer[+T] extends Property[T]

  sealed trait ListConsumer[+T] extends ListProperty[T]

  sealed trait MapConsumer[K, V] extends MapProperty[K, V]

  // see https://pulsar.apache.org/docs/2.10.x/client-libraries-java/#configure-consumer
  object Consumer:
    final case class topicNames[T <: String](value: List[T]) extends ListConsumer[T]

    final case class topicsPattern[T <: Pattern](value: T) extends Consumer[T]

    final case class subscriptionName[T <: String](value: T) extends Consumer[T]

    final case class subscriptionType[K <: SubscriptionKind](value: SubscriptionType[K])
        extends Property[SubscriptionType[K]]

    final case class receiverQueueSize[T <: Int](value: T) extends Consumer[T]

    final case class acknowledgementsGroupTimeMicros[T <: Long](value: T) extends Consumer[T]

    final case class negativeAckRedeliveryDelayMicros[T <: Long](value: T) extends Consumer[T]

    final case class maxTotalReceiverQueueSizeAcrossPartitions[T <: Int](value: T) extends Consumer[T]

    final case class consumerName[T <: String](value: T) extends Consumer[T]

    final case class ackTimeoutMillis[T <: Long](value: T) extends Consumer[T]

    final case class tickDurationMillis[T <: Long](value: T) extends Consumer[T]

    final case class priorityLevel[T <: Int](value: T) extends Consumer[T]

    final case class cryptoFailureAction[T <: ConsumerCryptoFailureAction](value: T) extends Consumer[T]

    final case class properties[K <: String, V <: String](value: SortedMap[K, V]) extends MapConsumer[K, V]

    final case class readCompacted[T <: Boolean](value: T) extends Consumer[T]

    final case class subscriptionInitialPosition[T <: SubscriptionInitialPosition](value: T) extends Consumer[T]

    final case class patternAutoDiscoveryPeriod[T <: Int](value: T) extends Consumer[T]

    final case class regexSubscriptionMode[T <: RegexSubscriptionMode](value: T) extends Consumer[T]

    final case class deadLetterPolicy[T <: DeadLetterPolicy](value: T) extends Consumer[T]

    final case class autoUpdatePartitions[T <: Boolean](value: T) extends Consumer[T]

    final case class replicateSubscriptionState[T <: Boolean](value: T) extends Consumer[T]

    final case class negativeAckRedeliveryBackoff[T <: RedeliveryBackoff](value: T) extends Consumer[T]

    final case class ackTimeoutRedeliveryBackoff[T <: RedeliveryBackoff](value: T) extends Consumer[T]

    final case class autoAckOldestChunkedMessageOnQueueFull[T <: Boolean](value: T) extends Consumer[T]

    final case class maxPendingChunkedMessage[T <: Int](value: T) extends Consumer[T]

    final case class expireTimeOfIncompleteChunkedMessageMillis[T <: Long](value: T) extends Consumer[T]
  end Consumer

  object Producer:
    // see https://pulsar.apache.org/docs/2.10.x/client-libraries-java/#configure-producer
    final case class topicName[T <: String](value: T) extends Producer[T]

    final case class producerName[T <: String](value: T) extends Producer[T]

    final case class sendTimeoutMs[T <: Long](value: T) extends Producer[T]

    final case class blockIfQueueFull[T <: Boolean](value: T) extends Producer[T]

    final case class maxPendingMessages[T <: Int](value: T) extends Producer[T]

    final case class maxPendingMessagesAcrossPartitions[T <: Int](value: T) extends Producer[T]

    final case class messageRoutingMode[T <: MessageRoutingMode](value: T) extends Producer[T]

    final case class hashingScheme[T <: HashingScheme](value: T) extends Producer[T]

    final case class cryptoFailureAction[T <: ProducerCryptoFailureAction](value: T) extends Producer[T]

    final case class batchingMaxPublishDelayMicros[T <: Long](value: T) extends Producer[T]

    final case class batchingMaxMessages[T <: Int](value: T) extends Producer[T]

    final case class batchingEnabled[T <: Boolean](value: T) extends Producer[T]

    final case class chunkingEnabled[T <: Boolean](value: T) extends Producer[T]

    final case class compressionType[T <: CompressionType](value: T) extends Producer[T]

    final case class initialSubscriptionName[T <: String](value: T) extends Producer[T]
  end Producer
end Property
