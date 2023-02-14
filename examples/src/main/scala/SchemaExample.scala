package examples

import zio._
import zio.pulsar._
import org.apache.pulsar.client.api.{ PulsarClientException, RegexSubscriptionMode, Schema => JSchema }
import RegexSubscriptionMode._
import com.sksamuel.avro4s.{ AvroSchema, SchemaFor }
import zio.json.DeriveJsonCodec
import zio.pulsar.json._
import zio.json.JsonCodec

case class User(email: String, name: Option[String], age: Int)

object SchemaExample extends App:

  def run(args: List[String]) =
    app.provideLayer(pulsarClient ++ Scope.default).exitCode

  val pulsarClient = PulsarClient.live("localhost", 6650)

  val topic = "my-schema-example-topic"

  given jsonCodec: JsonCodec[User] = DeriveJsonCodec.gen[User]

  val app: ZIO[PulsarClient & Scope, PulsarClientException, Unit] =
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
      _               = println(m.getValue)
    yield ()
