package zio.pulsar

import org.apache.pulsar.client.api.{ RegexSubscriptionMode, SubscriptionMode, SubscriptionType }

trait SubscriptionProperties

case class SimpleSubscriptionProperties(name: String, `type`: SubscriptionType, mode: SubscriptionMode)       extends SubscriptionProperties
case class PatternSubscriptionProperties(name: String, `type`: SubscriptionType, mode: RegexSubscriptionMode) extends SubscriptionProperties

trait Subscription

object Subscription {
  case class SingleSubscription(topic: String, subscription: SimpleSubscriptionProperties)          extends Subscription
  case class MultiSubscription(topics: List[String], subscription: SimpleSubscriptionProperties)    extends Subscription
  case class PatternSubscription(topicsPattern: String, subscription: PatternSubscriptionProperties) extends Subscription
}
