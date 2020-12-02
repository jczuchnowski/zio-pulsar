package zio.pulsar

import org.apache.pulsar.client.api.{ Message, Consumer => JConsumer, PulsarClientException }
import zio.{ RIO, ZIO, ZManaged }
import zio.blocking.Blocking

import scala.jdk.CollectionConverters._

final class Consumer private (val consumer: JConsumer[Array[Byte]]) {

  def receiveOne: RIO[Blocking, Message[Array[Byte]]] =
    ZIO.fromFutureJava(consumer.receiveAsync)

}

object Consumer {

  def subscribe(subscription: Subscription): ZManaged[PulsarClient, PulsarClientException, Consumer] = {
    val consumer = PulsarClient.client.flatMap { client =>
      val base = client.newConsumer
      val builder = subscription match {
        case Subscription.SingleSubscription(topic, props) =>
          base
            .topic(topic)
            .subscriptionName(props.name)
            .subscriptionType(props.`type`)
            .subscriptionMode(props.mode)
        case Subscription.MultiSubscription(topics, props) =>
          base
            .topics(topics.asJava)
            .subscriptionName(props.name)
            .subscriptionType(props.`type`)
            .subscriptionMode(props.mode)
        case Subscription.PatternSubscription(pattern, props) =>
          base
            .topicsPattern(pattern)
            .subscriptionName(props.name)
            .subscriptionType(props.`type`)
            .subscriptionTopicsMode(props.mode)
      }
      ZIO.effect(new Consumer(builder.subscribe)).refineToOrDie[PulsarClientException]
    }
    ZManaged.make(consumer)(p => ZIO.effect(p.consumer.close).orDie)
  }

}
