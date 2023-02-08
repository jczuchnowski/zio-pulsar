package examples

import org.apache.pulsar.client.api.{ PulsarClientException, Schema => JSchema }
import zio._
import zio.pulsar._
import zio.stream._

object StreamingExample extends App:

  def run(args: List[String]) =
    app.provideLayer(layer ++ Scope.default).exitCode

  val pulsarClient = PulsarClient.live("localhost", 6650)

  val layer = ZLayer.fromZIO(ZIO.succeed(Console.ConsoleLive)) >+> pulsarClient

  val topic = "my-topic"

  val producer: ZIO[PulsarClient & Scope, PulsarClientException, Unit] =
    for
      sink  <- Producer.make(topic, JSchema.STRING).map(_.asSink)
      stream = zio.stream.ZStream.fromIterable(0 to 100).map(i => s"Message $i")
      _     <- stream.run(sink)
    yield ()

  val consumer: ZIO[PulsarClient & Scope, PulsarClientException, Unit] =
    for
      builder  <- ConsumerBuilder.make(JSchema.STRING)
      consumer <- builder
                    .subscription(Subscription("my-subscription", SubscriptionType.Exclusive))
                    .topic(topic)
                    .build
      _        <- consumer.receiveStream.take(10).foreach { a =>
                    consumer.acknowledge(a.getMessageId)
                  }
    yield ()

  val app =
    for
      f <- consumer.fork
      _ <- producer
      _ <- f.join
    yield ()
