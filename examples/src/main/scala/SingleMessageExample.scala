package examples

import zio._
import zio.clock._
import zio.console._
import zio.pulsar._
import org.apache.pulsar.client.api.{ PulsarClientException, RegexSubscriptionMode }
import RegexSubscriptionMode._

object SingleMessageExample extends App {

  def run(args: List[String]): URIO[ZEnv, ExitCode] =
    app.provideCustomLayer(pulsarClient).useNow.exitCode

  val pulsarClient = PulsarClient.live("localhost", 6650)

  val topic = "my-topic"

  import zio.pulsar.given

  val app: ZManaged[PulsarClient, PulsarClientException, Unit] =
    for
      builder  <- ConsumerBuilder.make[String].toManaged_
      consumer <- builder
                    .withTopic(topic)
                    .withSubscription(
                      Subscription(
                        "my-subscription", 
                        SubscriptionType.Shared))
                    .build
      producer <- Producer.make[String](topic)
      _        <- producer.send("Hello!").toManaged_
      m        <- consumer.receive.toManaged_
    yield ()

}
