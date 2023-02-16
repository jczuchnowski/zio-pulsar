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
  import ObjectProperty._
  import NumberProperty._

  def and(other: Properties): Properties =
    if (propertyList.isEmpty) other
    else if (other.propertyList.isEmpty) this
    else if (other.propertyList.tail.isEmpty) Properties(other.propertyList.head :: propertyList)
    else Properties(other.propertyList ::: propertyList)

  def and(other: Property[_]): Properties =
    Properties(other :: propertyList)

  def getStringProperties: Map[String, String] = getProperties.map(f => f._1 -> f._2.toString)

  def getProperties: Map[String, Any] =
    this.propertyList
      .filter(_ != null)
      .foldLeft(List.empty[(String, Any)]) { (op, a) =>
        op.::(
          a match
            case _: NumberProperty[_] => a.name -> a.value
            case o: ObjectProperty[_] =>
              o match
                case subscriptionType(value) => a.name -> value.asJava.name()
                case topicsPattern(value)    => a.name -> value.pattern()
                case _                       => a.name -> a.value
            case _                    => a.name -> a.value
        )
      }
      .toMap
end Properties

object Properties:

  sealed trait Property[+T]:
    self =>
    import ObjectProperty._
    import NumberProperty._

    def name: String = self match
      case serviceUrl(_)         => serviceUrl.getClass.getSimpleName
      case topicName(_)          => topicName.getClass.getSimpleName
      case producerName(_)       => producerName.getClass.getSimpleName
      case consumerName(_)       => consumerName.getClass.getSimpleName
      case sendTimeoutMs(_)      => sendTimeoutMs.getClass.getSimpleName
      case maxPendingMessages(_) => maxPendingMessages.getClass.getSimpleName
      case topicsPattern(_)      => topicsPattern.getClass.getSimpleName
      case subscriptionName(_)   => subscriptionName.getClass.getSimpleName
      case subscriptionType(_)   => subscriptionType.getClass.getSimpleName
      case ackTimeoutMillis(_)   => ackTimeoutMillis.getClass.getSimpleName
      case receiverQueueSize(_)  => receiverQueueSize.getClass.getSimpleName
      case _                     => "invalid"

    def value: T

  // TODO add remaining
  sealed trait ObjectProperty[+T] extends Property[T]
  object ObjectProperty:
    final case class serviceUrl[+T <: String](value: T)       extends ObjectProperty[T]
    final case class topicName[+T <: String](value: T)        extends ObjectProperty[T]
    final case class producerName[+T <: String](value: T)     extends ObjectProperty[T]
    final case class consumerName[+T <: String](value: T)     extends ObjectProperty[T]
    final case class topicsPattern[+T <: Pattern](value: T)   extends ObjectProperty[T]
    final case class subscriptionName[+T <: String](value: T) extends ObjectProperty[T]
    final case class subscriptionType[K <: SubscriptionKind](value: SubscriptionType[K])
        extends ObjectProperty[SubscriptionType[K]]
  end ObjectProperty

  sealed trait NumberProperty[+T <: AnyVal] extends Property[T]
  object NumberProperty:
    final case class sendTimeoutMs[+T <: AnyVal](value: T)      extends NumberProperty[T]
    final case class maxPendingMessages[+T <: AnyVal](value: T) extends NumberProperty[T]
    final case class ackTimeoutMillis[+T <: AnyVal](value: T)   extends NumberProperty[T]
    final case class receiverQueueSize[+T <: AnyVal](value: T)  extends NumberProperty[T]
  end NumberProperty

end Properties
