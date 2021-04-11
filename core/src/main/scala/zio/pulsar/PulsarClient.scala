package zio.pulsar

import org.apache.pulsar.client.api.{ PulsarClient => JPulsarClient, PulsarClientException }
import zio._
  
type PulsarClient = Has[PulsarClient.Service]

object PulsarClient:

  trait Service:
    def client: IO[PulsarClientException, JPulsarClient]

  def live(host: String, port: Int): ULayer[PulsarClient] =
    ZLayer.fromManaged {
      val builder = JPulsarClient.builder().serviceUrl(s"pulsar://$host:$port")
  
      val cl = new Service {
        val client = ZIO.effect(builder.build).refineToOrDie[PulsarClientException]
      }
  
      ZManaged.make(ZIO.effectTotal(cl))(c => c.client.map(_.close()).orDie)
    }

  def make: ZIO[PulsarClient, PulsarClientException, JPulsarClient] =
    ZIO.accessM[PulsarClient](_.get.client)
