//package zio.pulsar

// import org.apache.pulsar.client.api.{ PulsarClient => JPulsarClient, PulsarClientException }
// import zio.{ Managed, ZIO, ZManaged }

// final class PulsarClient private[pulsar](val client: JPulsarClient)

// object PulsarClient:

//   def make(host: String, port: Int): Managed[PulsarClientException, PulsarClient] =
//     val builder = JPulsarClient.builder().serviceUrl(s"pulsar://$host:$port")
//     val client = ZIO.effect(new PulsarClient(builder.build)).refineToOrDie[PulsarClientException]
//     ZManaged.make(client)(c => ZIO.effect(c.client.close).orDie)
