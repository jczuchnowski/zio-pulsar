package examples

import zio._
import zio.pulsar._
import zio.stm._
import zio.stream._

import org.apache.pulsar.client.api.{
  MessageId,
  Producer => JProducer,
  PulsarClient => JPulsarClient,
  PulsarClientException,
  Schema => JSchema
}

object FanoutStreamExample extends App:

  def run(args: List[String]) = app.provideLayer(layer ++ Scope.default).exitCode

  val pulsarClient = PulsarClient.live("localhost", 6650)

  val layer = ZLayer.fromZIO(ZIO.succeed(Console.ConsoleLive)) ++ pulsarClient

  val pattern = "dynamic-topic-"

  val producer: ZIO[PulsarClient & Scope, PulsarClientException, Unit] =
    for
      sink  <- DynamicProducer.make(bytes => s"$pattern${new String(bytes).toInt % 5}").map(_.asSink)
      stream = zio.stream.ZStream.fromIterable(0 to 100).map(i => i.toString.getBytes)
      _     <- stream.run(sink)
    yield ()

  val consumer: ZIO[PulsarClient & Console & Scope, Throwable, Unit] =
    for
      builder  <- ConsumerBuilder.make(JSchema.STRING)
      consumer <- builder
                    .subscription(Subscription("my-subscription", SubscriptionType.Exclusive))
                    .pattern(s"$pattern.*")
                    .build
      _        <- consumer.receiveStream.take(10).foreach { a =>
                    Console.printLine("Received: (id: " + a.getMessageId + ") " + a.getValue) *>
                      consumer.acknowledge(a.getMessageId)
                  }
      _        <- Console.printLine("Finished")
    yield ()

  val app =
    for
      f <- consumer.fork
      _ <- producer
      _ <- f.join
    yield ()

final class DynamicProducer private (val client: JPulsarClient, val f: Array[Byte] => String):

  private val cache: collection.mutable.Map[String, JProducer[Array[Byte]]] = collection.mutable.Map.empty

  def send(message: Array[Byte]): IO[PulsarClientException, MessageId] =
    ZIO.attempt {
      val topic    = f(message)
      val producer = cache.getOrElse(topic, client.newProducer.topic(topic).create)
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
