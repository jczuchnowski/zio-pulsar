package zio.pulsar

import org.apache.pulsar.client.api.{ KeySharedPolicy, RegexSubscriptionMode, SubscriptionInitialPosition, SubscriptionMode }

case class Subscription(name: String, `type`: Option[SubscriptionType] = None, initialPosition: Option[SubscriptionInitialPosition] = None, properties: SubscriptionProperties)

trait SubscriptionType

object SubscriptionType {
  final case class Exclusive(readCompacted: Boolean = false) extends SubscriptionType
  case object Shared                                         extends SubscriptionType
  final case class Failover(readCompacted: Boolean = false)  extends SubscriptionType
  final case class KeyShared(policy: KeySharedPolicy)        extends SubscriptionType
}

trait SubscriptionProperties

object SubscriptionProperties {
  case class TopicSubscriptionProperties(topics: List[String], mode: Option[SubscriptionMode] = None) extends SubscriptionProperties
  case class PatternSubscriptionProperties(topicsPattern: String, mode: Option[RegexSubscriptionMode] = None, patternAutoDiscoveryPeriod: Option[Int] = None)
      extends SubscriptionProperties
}
