package official_client

import org.apache.pulsar.client.api._
import org.apache.pulsar.client.impl.schema.JSONSchema

case class User(name: String, age: Int)

@main
def schemaExample = 
  val client = PulsarClient.builder.serviceUrl("pulsar://localhost:6650").build
  val producer = client.newProducer(JSONSchema.of(classOf[User])).topic("users").create
  producer.send(User("Jack", 33))

  val consumer = client.newConsumer(JSONSchema.of(classOf[User])).topic("users").subscriptionName("my-subscription").subscribe
  val message = consumer.receive
  println(message.getValue)