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
  Schema
}

object FanoutStreamExample extends App:
  
  def run(args: List[String]): URIO[ZEnv, ExitCode] =
    app.provideCustomLayer(layer).useNow.exitCode

  val pulsarClient = PulsarClient.live("localhost", 6650)

  val layer = ((Console.live ++ Clock.live)) >+> pulsarClient

  val pattern = "dynamic-topic-"

  val producer: ZManaged[Has[PulsarClient], PulsarClientException, Unit] = 
    for
      sink   <- DynamicProducer.make(bytes => s"$pattern${new String(bytes).toInt%5}").map(_.asSink)
      stream =  Stream.fromIterable(0 to 100).map(i => i.toString.getBytes)
      _      <- stream.run(sink).toManaged_
    yield ()

  val consumer: ZManaged[Has[PulsarClient] with Has[Console], Throwable, Unit] =
    for
      builder  <- ConsumerBuilder.make(Schema.STRING).toManaged_
      consumer <- builder
                    .subscription(Subscription("my-subscription", SubscriptionType.Exclusive))
                    .pattern(s"$pattern.*")
                    .build
      _        <- consumer.receiveStream.take(10).foreach { a => 
                    Console.putStrLn("Received: (id: " + a.getMessageId + ") " + a.getValue) *>
                    consumer.acknowledge(a.getMessageId)
                  }.toManaged_
      _        <- Console.putStrLn("Finished").toManaged_
    yield ()

  val app =
    for
      f <- consumer.fork
      _ <- producer
      _ <- f.join.toManaged_
    yield ()

final class DynamicProducer private (val client: JPulsarClient, val f: Array[Byte] => String):

  private val cache: collection.mutable.Map[String, JProducer[Array[Byte]]] = collection.mutable.Map.empty

  def send(message: Array[Byte]): IO[PulsarClientException, MessageId] =
    ZIO.effect {
      val topic = f(message)
      val producer = cache.getOrElse(topic, client.newProducer.topic(topic).create)
      val m = producer.send(message)
      cache + (topic -> producer)
      m
    }.refineToOrDie[PulsarClientException]

  def asSink = ZSink.foreach[Any, PulsarClientException, Array[Byte]] { m => send(m) }

object DynamicProducer:

  def make(f: Array[Byte] => String): ZManaged[Has[PulsarClient], PulsarClientException, DynamicProducer] =
    val producer = PulsarClient.make.map { client =>
      DynamicProducer(client, f)
    }

    ZManaged.make(producer)(p => ZIO.effect(p.cache.values.foreach(_.close)).orDie)

