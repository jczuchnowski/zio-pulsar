package examples

import zio._
import zio.pulsar._
import org.apache.pulsar.client.api.{ PulsarClientException, RegexSubscriptionMode, Schema }
import RegexSubscriptionMode._
import com.sksamuel.avro4s.{ AvroSchema, SchemaFor }
import zio.json.DeriveJsonCodec
import zio.pulsar.json.given
import zio.json.JsonCodec

case class User(email: String, name: Option[String], age: Int)

object SchemaExample extends App:

  def run(args: List[String]): URIO[ZEnv, ExitCode] =
    app.provideCustomLayer(pulsarClient).useNow.exitCode

  val pulsarClient = PulsarClient.live("localhost", 6650)

  val topic = "my-topic"

  given jsonCodec: JsonCodec[User] = DeriveJsonCodec.gen[User]

  val app: ZManaged[PulsarClient, PulsarClientException, Unit] =
    for
      builder  <- ConsumerBuilder.make(using jsonSchema[User]).toManaged_
      consumer <- builder
                    .topic(topic)
                    .subscription(
                      Subscription(
                        "my-subscription", 
                        SubscriptionType.Shared))
                    .build
      producer <- Producer.make(topic)(using jsonSchema[User])
      _        <- producer.send(User("test@test.com", None, 25)).toManaged_
      m        <- consumer.receive.toManaged_
      _        = println(m.getValue)
    yield ()
