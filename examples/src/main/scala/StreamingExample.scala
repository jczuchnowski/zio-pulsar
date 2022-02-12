package examples

import org.apache.pulsar.client.api.{ PulsarClientException, Schema => JSchema}
import zio._
import zio.pulsar._
import zio.stream._

object StreamingExample extends App:

  def run(args: List[String]): URIO[ZEnv, ExitCode] =
    app.provideCustomLayer(layer).useNow.exitCode

  val pulsarClient = PulsarClient.live("localhost", 6650)

  val layer = ((Console.live ++ Clock.live)) >+> pulsarClient

  val topic = "my-topic"

  val producer: ZManaged[PulsarClient, PulsarClientException, Unit] = 
    for
      sink   <- Producer.make(topic, JSchema.STRING).map(_.asSink)
      stream =  Stream.fromIterable(0 to 100).map(i => s"Message $i")
      _      <- stream.run(sink).toManaged_
    yield ()

  val consumer: ZManaged[PulsarClient, PulsarClientException, Unit] =
    for
      builder  <- ConsumerBuilder.make(JSchema.STRING).toManaged_
      consumer <- builder
                    .subscription(Subscription("my-subscription", SubscriptionType.Exclusive))
                    .topic(topic)
                    .build
      _        <- consumer.receiveStream.take(10).foreach { a => 
                    consumer.acknowledge(a.getMessageId)
                  }.toManaged_
    yield ()

  val app =
    for
      f <- consumer.fork
      _ <- producer
      _ <- f.join.toManaged_
    yield ()
