package official_client

import org.apache.pulsar.client.api._
import org.apache.pulsar.client.impl.schema.JSONSchema

case class User(name: String, age: Int)

class UserPOJO:
  private var age: Int = 0
  private var name: String = ""

  def getAge: Int = this.age
  def getName: String = this.name
  def setAge(age: Int): Unit = this.age = age  
  def setName(name: String): Unit = this.name = name
  
val userPOJO = 
  val u = new UserPOJO
  u.setAge(33)
  u.setName("Jack")
  u

val topic = "my-schema-pojo-example-topic"

@main
def schemaExample = 
  val client = PulsarClient.builder.serviceUrl("pulsar://localhost:6650").build
  val producer = client.newProducer(JSONSchema.of(classOf[UserPOJO])).topic(topic).create
  val sMsgId = producer.send(userPOJO)
  
  println("--------------")
  println(s"sent $sMsgId")
  println("--------------")

  val consumer = client.newConsumer(JSONSchema.of(classOf[UserPOJO])).topic(topic).subscriptionName("my-schema-pojo-example-subscription").subscribe
  val message = consumer.receiveAsync.get
  val rMsgId = message.getMessageId
  consumer.acknowledge(rMsgId)
  
  println("-------------")
  println(s"received ${rMsgId}")
  println(message.getValue)
  println("-------------")
  
  consumer.close
  producer.close
  client.close
