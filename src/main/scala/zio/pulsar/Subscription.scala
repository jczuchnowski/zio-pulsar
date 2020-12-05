package zio.pulsar

import org.apache.pulsar.client.api.{ KeySharedPolicy, RegexSubscriptionMode, SubscriptionInitialPosition, SubscriptionMode }

trait SubscriptionType

object SubscriptionType {
  final case class Exclusive(readCompacted: Boolean = false) extends SubscriptionType
  case object Shared                                         extends SubscriptionType
  final case class Failover(readCompacted: Boolean = false)  extends SubscriptionType
  final case class KeyShared(policy: KeySharedPolicy)        extends SubscriptionType
}

trait SubscriptionProperties

case class SimpleSubscriptionProperties(name: String, `type`: SubscriptionType, mode: SubscriptionMode, initialPosition: SubscriptionInitialPosition)
    extends SubscriptionProperties
case class PatternSubscriptionProperties(name: String, `type`: SubscriptionType, mode: RegexSubscriptionMode, initialPosition: SubscriptionInitialPosition, patternAutoDiscoveryPeriod: Int)
    extends SubscriptionProperties

trait Subscription

object Subscription {
  case class SingleSubscription(topic: String, subscription: SimpleSubscriptionProperties)       extends Subscription
  case class MultiSubscription(topics: List[String], subscription: SimpleSubscriptionProperties) extends Subscription
  case class PatternSubscription(topicsPattern: String, subscription: PatternSubscriptionProperties)
      extends Subscription
}
