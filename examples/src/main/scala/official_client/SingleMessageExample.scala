package official_client

import org.apache.pulsar.client.api._

@main
def singleMessageExample =
  val client   = PulsarClient.builder.serviceUrl("pulsar://localhost:6650").build
  val consumer = client.newConsumer
    .topic("my-topic")
    .subscriptionName("my-subscription")
    .subscriptionType(SubscriptionType.Exclusive)
    .subscribe
  val producer = client.newProducer.topic("my-topic").create

  producer.send("Hello!".getBytes)
  val msg = consumer.receive

  producer.close
  consumer.close
  client.close
