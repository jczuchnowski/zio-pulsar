package workshop

import zio._
import zio.blocking._
import zio.clock._
import zio.console._
import zio.pulsar._
import zio.stm._
import zio.stream._

import org.apache.pulsar.client.api.{ 
  MessageId, 
  Producer => JProducer, 
  PulsarClient => JPulsarClient, 
  PulsarClientException
}

object FanoutStreamExample extends App:
  
  def run(args: List[String]): URIO[ZEnv, ExitCode] =
    app.provideCustomLayer(layer).useNow.exitCode

  val pulsarClient = PulsarClient.live("localhost", 6650)

  val layer = ((Console.live ++ Clock.live)/* >>> logger */) >+> pulsarClient

  val pattern = "dynamic-topic-"

  val producer: ZManaged[PulsarClient, PulsarClientException, Unit] = 
    for {
      sink   <- DynamicProducer.make.map(_.asSink)
      stream = Stream.fromIterable(0 to 100).map(i => (s"$pattern${i%5}", s"Message $i".getBytes))
      _      <- stream.run(sink).toManaged_
    } yield ()

  val consumer: ZManaged[PulsarClient with Console with Blocking, Throwable, Unit] =
    for {
      client <- PulsarClient.make.toManaged_
      c   <- ConsumerBuilder(client)
               .withSubscription(Subscription("my-subscription", SubscriptionType.Exclusive))
               .withPattern(s"$pattern*")
               .build
      _ <- c.receiveStream.take(10).foreach { a => 
            putStrLn("Received: (id: " + a.getMessageId.toString + ") " + a.getData().map(_.toChar).mkString) *>
              c.acknowledge(a.getMessageId())
            }.toManaged_
      _ <- putStrLn("Finished").toManaged_
    } yield ()

  val app =
    for {
      f <- consumer.fork
      _ <- producer
      _ <- f.join.toManaged_
    } yield ()

final class DynamicProducer private (val client: JPulsarClient):

  private val cache: collection.mutable.Map[String, JProducer[Array[Byte]]] = collection.mutable.Map.empty

  def send(topic: String, message: Array[Byte]): IO[PulsarClientException, MessageId] =
    ZIO.effect {
      val producer = cache.getOrElse(topic, client.newProducer.topic(topic).create)
      val m = producer.send(message)
      cache + (topic -> producer)
      m
    }.refineToOrDie[PulsarClientException]

  def asSink = ZSink.foreach[Any, PulsarClientException, (String, Array[Byte])] { case (t, m) => send(t, m) }

object DynamicProducer:

  val make: ZManaged[PulsarClient, PulsarClientException, DynamicProducer] =
    val producer = PulsarClient.make.map { client =>
      DynamicProducer(client)
    }

    ZManaged.make(producer)(p => ZIO.effect(p.cache.values.foreach(_.close)).orDie)

