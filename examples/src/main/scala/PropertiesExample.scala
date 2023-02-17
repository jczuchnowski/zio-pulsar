import org.apache.pulsar.client.api.RegexSubscriptionMode.*
import org.apache.pulsar.client.api.{ PulsarClientException, RegexSubscriptionMode, Schema as JSchema }
import zio.*
import zio.pulsar.*

import java.io.IOException

object PropertiesExample extends ZIOAppDefault:

  val pulsarClient = PulsarClient.live("localhost", 6650)

  val topic = "properties-topic"

  import Properties.*
  import Property.*
  import Property.Consumer.*
  import Property.Producer.*

  val app: ZIO[PulsarClient with Scope, IOException, Unit] =
    for
      builder        <- ConsumerBuilder.make(JSchema.STRING)
      consumer       <- builder
                          .topic(topic)
                          .loadConf(ConsumerProperties(consumerName("helloworld-consumer")))
                          .properties(
                            StringProperty("", "")
                          )
                          .subscription(Subscription("my-subscription", SubscriptionType.Shared))
                          .build
      productBuilder <- ProducerBuilder.make(JSchema.STRING)
      producer       <- productBuilder
                          .topic(topic)
                          .properties(
                            StringProperty("", "")
                          )
                          .loadConf(ProducerProperties(producerName("helloworld-consumer")))
                          .build
      _              <- producer.send("Hello!")
      m              <- consumer.receive
      _              <- Console.printLine(m.getValue)
    yield ()

  override def run = app.provideLayer(pulsarClient ++ Scope.default).exitCode
