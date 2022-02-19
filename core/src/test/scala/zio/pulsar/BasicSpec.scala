package zio.pulsar

import zio._
import zio.json._
import zio.pulsar.json._
import zio.test.Assertion.equalTo
import zio.test.junit.JUnitRunnableSpec
import zio.test.{ assertM, suite, test }
import zio.test.TestAspect.sequential
import org.apache.pulsar.client.api.{ PulsarClientException, RegexSubscriptionMode, Schema => JSchema }
import java.time.LocalDate



object BasicSpec extends PulsarContainerSpec:

  case class Order(
    item: String, 
    price: BigDecimal, 
    quantity: Int, 
    description: Option[String], 
    comment: Option[String], 
    date: LocalDate)

  def specLayered = suite("PulsarClient")(
    test("send and receive String message") {
      val topic = "my-test-topic"
      val client = ZIO.accessM[PulsarClient](_.get.client)
      val result = for
        builder  <- ConsumerBuilder.make(JSchema.STRING).toManaged_
        consumer <- builder
                      .topic(topic)
                      .subscription(
                        Subscription(
                          "my-test-subscription", 
                          SubscriptionType.Exclusive))
                      .build
        producer <- Producer.make(topic, JSchema.STRING)
        _        <- producer.send("Hello!").toManaged_
        m        <- consumer.receive.toManaged_
      yield m

      assertM(result.useNow.map(_.getValue))(equalTo("Hello!"))
    },
    test("send and receive JSON message") {
      given jsonCodec: JsonCodec[Order] = DeriveJsonCodec.gen[Order]
      val topic = "my-test-topic-2"
      val message = Order("test item", 10.5, 5, Some("test description"), None, LocalDate.of(2000, 1, 1))
      val client = ZIO.accessM[PulsarClient](_.get.client)
      val result = for
        builder  <- ConsumerBuilder.make(Schema.jsonSchema[Order]).toManaged_
        consumer <- builder
                      .topic(topic)
                      .subscription(
                        Subscription(
                          "my-test-subscription-2", 
                          SubscriptionType.Exclusive))
                      .build
        producer <- Producer.make(topic, Schema.jsonSchema[Order])
        _        <- producer.send(message).toManaged_
        m        <- consumer.receive.toManaged_
      yield m

      assertM(result.useNow.map(_.getValue))(equalTo(message))
    }
  ) @@ sequential
