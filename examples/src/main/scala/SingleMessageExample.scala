package examples

import zio.*
import zio.pulsar.*
import org.apache.pulsar.client.api.{ PulsarClientException, RegexSubscriptionMode, Schema as JSchema }
import RegexSubscriptionMode.*
import examples.SchemaExample.{ app, pulsarClient }

object SingleMessageExample extends ZIOAppDefault:

  val pulsarClient = PulsarClient.live("localhost", 6650)

  val topic = "my-topic"

  val app: ZIO[PulsarClient & Scope, PulsarClientException, Unit] =
    for
      builder  <- ConsumerBuilder.make(JSchema.STRING)
      consumer <- builder
                    .topic(topic)
                    .subscription(Subscription("my-subscription", SubscriptionType.Shared))
                    .build
      producer <- Producer.make(topic, JSchema.STRING)
      _        <- producer.send("Hello!")
      m        <- consumer.receive
    yield ()

  override def run = app.provideLayer(pulsarClient ++ Scope.default).exitCode
