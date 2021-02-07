package zio.pulsar

import org.apache.pulsar.client.api.{ MessageId, Producer => JProducer, PulsarClientException }
import zio.{ IO, ZIO, ZManaged }

final class Producer private (val producer: JProducer[Array[Byte]]) {

  def send(message: Array[Byte]): IO[PulsarClientException, MessageId] =
    ZIO.effect(producer.send(message)).refineToOrDie[PulsarClientException]

}

object Producer {

  def make(topic: String): ZManaged[PulsarClient, PulsarClientException, Producer] = ???/**{
    val producer = PulsarClient.make.flatMap { client =>
      val builder = client.newProducer.topic(topic)
      ZIO.effect(new Producer(builder.create)).refineToOrDie[PulsarClientException]
    }
    ZManaged.make(producer)(p => ZIO.effect(p.producer.close).orDie)
  }*/
}
