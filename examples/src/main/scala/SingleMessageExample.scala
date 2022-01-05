package examples

import zio._
import zio.pulsar._
import org.apache.pulsar.client.api.{ PulsarClientException, RegexSubscriptionMode, Schema }
import RegexSubscriptionMode._

object SingleMessageExample extends App:

  def run(args: List[String]): URIO[ZEnv, ExitCode] =
    app.provideCustomLayer(pulsarClient).useNow.exitCode

  val pulsarClient = PulsarClient.live("localhost", 6650)

  val topic = "my-topic"

  val app: ZManaged[PulsarClient, PulsarClientException, Unit] =
    for
      builder  <- ConsumerBuilder.make(Schema.STRING).toManaged_
      consumer <- builder
                    .topic(topic)
                    .subscription(
                      Subscription(
                        "my-subscription", 
                        SubscriptionType.Shared))
                    .build
      producer <- Producer.make(topic, Schema.STRING)
      _        <- producer.send("Hello!").toManaged_
      m        <- consumer.receive.toManaged_
    yield ()
