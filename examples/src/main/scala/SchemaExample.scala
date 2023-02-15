package examples

import zio.*
import zio.pulsar.*
import org.apache.pulsar.client.api.{ PulsarClientException, RegexSubscriptionMode, Schema as JSchema }
import RegexSubscriptionMode.*
import com.sksamuel.avro4s.{ AvroSchema, SchemaFor }
import zio.json.DeriveJsonCodec
import zio.pulsar.json.*
import zio.json.JsonCodec

import java.io.IOException

case class User(email: String, name: Option[String], age: Int)

object SchemaExample extends ZIOAppDefault:

  val pulsarClient = PulsarClient.live("localhost", 6650)

  val topic = "my-schema-example-topic"

  given jsonCodec: JsonCodec[User] = DeriveJsonCodec.gen[User]

  val app: ZIO[PulsarClient with Scope, IOException, Unit] =
    for
      builder        <- ConsumerBuilder.make(Schema.jsonSchema[User])
      consumer       <- builder
                          .topic(topic)
                          .subscription(Subscription("my-schema-example-subscription", SubscriptionType.Shared))
                          .build
      productBuilder <- ProducerBuilder.make(Schema.jsonSchema[User])
      producer       <- productBuilder.topic(topic).build
      _              <- producer.send(User("test@test.com", None, 25))
      m              <- consumer.receive
      _              <- Console.printLine(m.getValue)
    yield ()

  override def run = app.provideLayer(pulsarClient ++ Scope.default).exitCode
