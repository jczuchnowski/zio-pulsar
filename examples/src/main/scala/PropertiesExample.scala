import org.apache.pulsar.client.api.RegexSubscriptionMode.*
import org.apache.pulsar.client.api.{ PulsarClientException, RegexSubscriptionMode, Schema as JSchema }
import zio.*
import zio.pulsar.*

import java.io.IOException

object PropertiesExample extends ZIOAppDefault:

  val pulsarClient = PulsarClient.live("localhost", 6650)

  val topic = "properties-topic"

  import Properties._
  import zio.pulsar.Properties.ConsumerProperty._
  import zio.pulsar.Properties.ProducerProperty._

  val app: ZIO[PulsarClient with Scope, IOException, Unit] =
    for
      builder        <- ConsumerBuilder.make(JSchema.STRING)
      consumer       <- builder
                          .topic(topic)
                          .loadConf(Properties(List(consumerName("helloworld-consumer"))))
                          .properties(
                            Properties.StringProperty("", "")
                          )
                          .subscription(Subscription("my-subscription", SubscriptionType.Shared))
                          .build
      productBuilder <- ProducerBuilder.make(JSchema.STRING)
      producer       <- productBuilder
                          .topic(topic)
                          .properties(
                            Properties.StringProperty("", "")
                          )
                          .loadConf(Properties(List(consumerName("helloworld-consumer"))))
                          .build
      _              <- producer.send("Hello!")
      m              <- consumer.receive
      _              <- Console.printLine(m.getValue)
    yield ()

  override def run = app.provideLayer(pulsarClient ++ Scope.default).exitCode
