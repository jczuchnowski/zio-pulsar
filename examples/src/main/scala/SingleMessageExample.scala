package examples

import zio._
import zio.pulsar._
import org.apache.pulsar.client.api.{ PulsarClientException, RegexSubscriptionMode, Schema => JSchema }
import RegexSubscriptionMode._

object SingleMessageExample extends App:

  def run(args: List[String]) =
    app.provideLayer(pulsarClient ++ Scope.default).exitCode

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
