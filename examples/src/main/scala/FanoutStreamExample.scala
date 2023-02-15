package examples

import zio.*
import zio.pulsar.*
import zio.stm.*
import zio.stream.*
import org.apache.pulsar.client.api.{
  MessageId,
  Producer as JProducer,
  PulsarClient as JPulsarClient,
  PulsarClientException,
  RegexSubscriptionMode,
  Schema as JSchema
}

import java.io.IOException
import java.util.regex.Pattern

object FanoutStreamExample extends ZIOAppDefault:

  val pulsarClient = PulsarClient.live("localhost", 6650)

  val producer: ZIO[PulsarClient & Scope, PulsarClientException, Unit] =
    for
      sink  <- DynamicProducer.make(bytes => s"pattern-topic-${new String(bytes).toInt % 5}").map(_.asSink)
      stream = zio.stream.ZStream.fromIterable(0 to 100).map(i => i.toString.getBytes)
      _     <- stream.run(sink)
    yield ()

  val pattern = Pattern.compile("persistent://public/default/pattern-topic-.*")

  val consumer: ZIO[PulsarClient with Scope, IOException, Unit] =
    for
      builder  <- ConsumerBuilder.make(JSchema.BYTES)
      consumer <- builder
                    .pattern(pattern)
                    .subscription(Subscription("my-subscription", SubscriptionType.Exclusive))
                    .subscriptionTopicsMode(RegexSubscriptionMode.PersistentOnly)
                    .build
      _        <- consumer.receiveStream.take(10).foreach { a =>
                    Console.printLine("Received: (id: " + a.getMessageId + ") " + new String(a.getValue)) *>
                      consumer.acknowledge(a.getMessageId)
                  }
      _        <- Console.printLine("Finished")
    yield ()

  val app =
    for
      f <- consumer
      _ <- producer
      _ <- ZIO.never
    yield ()

  override def run = app.provideLayer(pulsarClient ++ Scope.default).exitCode

final class DynamicProducer private (val client: JPulsarClient, val f: Array[Byte] => String):

  private val cache: collection.mutable.Map[String, JProducer[Array[Byte]]] = collection.mutable.Map.empty

  def send(message: Array[Byte]): IO[PulsarClientException, MessageId] =
    ZIO.attempt {
      val topic    = f(message)
      val producer = cache.getOrElse(topic, client.newProducer(JSchema.BYTES).topic(topic).create)
      val m        = producer.send(message)
      cache + (topic -> producer)
      m
    }.refineToOrDie[PulsarClientException]

  def asSink = ZSink.foreach[Any, PulsarClientException, Array[Byte]](m => send(m))

object DynamicProducer:

  def make(f: Array[Byte] => String): ZIO[PulsarClient & Scope, PulsarClientException, DynamicProducer] =
    val producer = PulsarClient.make.map { client =>
      DynamicProducer(client, f)
    }

    ZIO.acquireRelease(producer)(p => ZIO.attempt(p.cache.values.foreach(_.close)).orDie)
