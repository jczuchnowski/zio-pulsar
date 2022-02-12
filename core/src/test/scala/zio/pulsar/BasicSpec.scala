package zio.pulsar

import zio._
import zio.test.Assertion.equalTo
import zio.test.junit.JUnitRunnableSpec
import zio.test.{ assertM, suite, test }
import org.apache.pulsar.client.api.{ PulsarClientException, RegexSubscriptionMode, Schema => JSchema }

object BasicSpec extends PulsarContainerSpec {

  def specLayered = suite("PulsarClient")(
    test("send and receive string message") {
      val topic = "my-topic"
      val client = ZIO.accessM[PulsarClient](_.get.client)
      val result = for
        builder  <- ConsumerBuilder.make(JSchema.STRING).toManaged_
        consumer <- builder
                      .topic(topic)
                      .subscription(
                        Subscription(
                          "my-subscription", 
                          SubscriptionType.Shared))
                      .build
        producer <- Producer.make(topic, JSchema.STRING)
        _        <- producer.send("Hello!").toManaged_
        m        <- consumer.receive.toManaged_
      yield m

      assertM(result.useNow.map(_.getValue))(equalTo("Hello!"))
    }
  )
}
