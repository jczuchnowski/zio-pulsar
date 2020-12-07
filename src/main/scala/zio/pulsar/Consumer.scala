package zio.pulsar

import org.apache.pulsar.client.api.{
  Message,
  Consumer => JConsumer,
  PulsarClient => JPulsarClient,
  PulsarClientException,
  SubscriptionType => JSubscriptionType
}
import zio.{ RIO, ZIO, ZManaged }
import zio.blocking.Blocking
import zio.stream._

import scala.jdk.CollectionConverters._
import org.apache.pulsar.client.api.ConsumerBuilder

trait Consumer

object Consumer {

  final class Simple private[Consumer] (val consumer: JConsumer[Array[Byte]]) extends Consumer {

    val receive: RIO[Blocking, Message[Array[Byte]]] =
      ZIO.fromFutureJava(consumer.receiveAsync)

  }

  final class Streaming private[Consumer] (val consumer: JConsumer[Array[Byte]])
      extends Consumer {

    val receive: Stream[Throwable, Message[Array[Byte]]] = 
      ZStream.fromEffect(ZIO.fromCompletionStage(consumer.receiveAsync))

  }

  private def subscriptionType(t: SubscriptionType, builder: ConsumerBuilder[Array[Byte]]): ConsumerBuilder[Array[Byte]] =
    t match {
      case SubscriptionType.Exclusive(r) => 
        builder
          .subscriptionType(JSubscriptionType.Exclusive)
          .readCompacted(r)

      case SubscriptionType.Failover(r)  => 
        builder
          .subscriptionType(JSubscriptionType.Failover)
          .readCompacted(r)

      case SubscriptionType.Shared       => 
        builder
          .subscriptionType(JSubscriptionType.Shared)
          .readCompacted(false)

      case SubscriptionType.KeyShared(p) => 
        builder
          .subscriptionType(JSubscriptionType.Key_Shared)
          .readCompacted(false)
          .keySharedPolicy(p)
    }

  private def consumerBuilder(client: JPulsarClient, subscription: Subscription) = {
    subscription match {
      case Subscription.SingleSubscription(topic, props) =>
        subscriptionType(props.`type`, client.newConsumer)
          .topic(topic)
          .subscriptionName(props.name)
          .subscriptionMode(props.mode)
        
      case Subscription.MultiSubscription(topics, props) =>
        subscriptionType(props.`type`, client.newConsumer)
          .topics(topics.asJava)
          .subscriptionName(props.name)
          .subscriptionMode(props.mode)

      case Subscription.PatternSubscription(pattern, props) =>
        subscriptionType(props.`type`, client.newConsumer)
          .topicsPattern(pattern)
          .subscriptionName(props.name)
          .subscriptionTopicsMode(props.mode)
          .patternAutoDiscoveryPeriod(props.patternAutoDiscoveryPeriod)
    }
  }

  def subscribe(subscription: Subscription): ZManaged[PulsarClient, PulsarClientException, Simple] = {
    val consumer = PulsarClient.client.map { client =>
      val builder = consumerBuilder(client, subscription)
      new Simple(builder.subscribe)
    }
    ZManaged.make(consumer)(p => ZIO.effect(p.consumer.close).orDie)
  }

  def streaming(subscription: Subscription): ZManaged[PulsarClient, PulsarClientException, Streaming] = {
    val consumer = for {
      client <- PulsarClient.client
      builder = consumerBuilder(client, subscription)
    } yield new Streaming(builder.subscribe)
    ZManaged.make(consumer)(p => ZIO.effect(p.consumer.close).orDie)
  }

}
