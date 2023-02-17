package zio.pulsar

import Properties.*
import org.apache.pulsar.client.api.SubscriptionType as JSubscriptionType

import java.util.regex.Pattern
import scala.reflect.{ classTag, ClassTag }

/**
 * @author
 *   梦境迷离
 * @version 1.0,2023/2/16
 */
final case class Properties(
  propertyList: List[Property[_]] = Nil
):
  import Properties._
  import ConsumerProperty.*
  import ProducerProperty.*

  def and(other: Properties): Properties =
    if (propertyList.isEmpty) other
    else if (other.propertyList.isEmpty) this
    else if (other.propertyList.tail.isEmpty) Properties(other.propertyList.head :: propertyList)
    else Properties(other.propertyList ::: propertyList)

  def and(other: Property[_]): Properties =
    Properties(other :: propertyList)

  def getProperties: Map[String, String] = getConfig.map(f => f._1 -> f._2.toString)

  def getConfig: Map[String, Any] =
    this.propertyList
      .filter(_ != null)
      .foldLeft(List.empty[(String, Any)]) { (op, a) =>
        op.:: {
          val key = a._key.getOrElse(a.name)
          a match
            case topicNames(value) => key -> value.toSet
            case _                 => key -> a.value
        }
      }
      .toMap
end Properties

object Properties:

  sealed trait Property[+T]:
    self =>
    def name: String         = self.getClass.getSimpleName
    def value: T
    def _key: Option[String] = None

  end Property

  final case class StringProperty(key: String, value: String) extends Property[String] {
    override def _key: Option[String] = Option(key)
  }

  sealed trait ListProperty[+T] extends Property[List[T]]:
    override def value: List[T]
  end ListProperty

  // TODO add remaining
  object ConsumerProperty:
    final case class topicNames[+T](value: List[T])                                extends ListProperty[T]
    final case class topicsPattern[T <: Pattern](value: T)                         extends Property[T]
    final case class subscriptionName[T <: String](value: T)                       extends Property[T]
    final case class subscriptionType[K <: SubscriptionKind](value: SubscriptionType[K])
        extends Property[SubscriptionType[K]]
    final case class receiverQueueSize[T <: Int](value: T)                         extends Property[T]
    final case class acknowledgementsGroupTimeMicros[T <: Long](value: T)          extends Property[T]
    final case class negativeAckRedeliveryDelayMicros[T <: Long](value: T)         extends Property[T]
    final case class maxTotalReceiverQueueSizeAcrossPartitions[T <: Int](value: T) extends Property[T]
    final case class consumerName[T <: String](value: T)                           extends Property[T]
    final case class ackTimeoutMillis[T <: Long](value: T)                         extends Property[T]
    final case class tickDurationMillis[T <: Long](value: T)                       extends Property[T]
    final case class priorityLevel[T <: Int](value: T)                             extends Property[T]
  end ConsumerProperty

  object ProducerProperty:
    final case class topicName[T <: String](value: T)                       extends Property[T]
    final case class producerName[T <: String](value: T)                    extends Property[T]
    final case class sendTimeoutMs[T <: Long](value: T)                     extends Property[T]
    final case class blockIfQueueFull[T <: Boolean](value: T)               extends Property[T]
    final case class maxPendingMessages[T <: Int](value: T)                 extends Property[T]
    final case class maxPendingMessagesAcrossPartitions[T <: Int](value: T) extends Property[T]
  end ProducerProperty

end Properties
