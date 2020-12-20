package zio.pulsar

import org.apache.pulsar.client.api.{ KeySharedPolicy, RegexSubscriptionMode, SubscriptionInitialPosition, SubscriptionMode }

case class Subscription(name: String, `type`: SubscriptionType, initialPosition: SubscriptionInitialPosition, properties: SubscriptionProperties)

trait SubscriptionType

object SubscriptionType {
  final case class Exclusive(readCompacted: Boolean = false) extends SubscriptionType
  case object Shared                                         extends SubscriptionType
  final case class Failover(readCompacted: Boolean = false)  extends SubscriptionType
  final case class KeyShared(policy: KeySharedPolicy)        extends SubscriptionType
}

trait SubscriptionProperties

object SubscriptionProperties {
  case class TopicSubscriptionProperties(topics: List[String], mode: SubscriptionMode) extends SubscriptionProperties
  case class PatternSubscriptionProperties(topicsPattern: String, mode: RegexSubscriptionMode, patternAutoDiscoveryPeriod: Int)
      extends SubscriptionProperties
}
