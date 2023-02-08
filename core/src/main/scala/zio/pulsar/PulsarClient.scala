package zio.pulsar

import org.apache.pulsar.client.api.{ PulsarClient => JPulsarClient, PulsarClientException }
import zio._

trait PulsarClient:
  def client: IO[PulsarClientException, JPulsarClient]

object PulsarClient:

  def live(url: String): ULayer[PulsarClient] =
    val builder = JPulsarClient.builder().serviceUrl(url)

    val cl = new PulsarClient {
      val client = ZIO.attempt(builder.build).refineToOrDie[PulsarClientException]
    }

    ZLayer(ZIO.scoped(ZIO.acquireRelease(ZIO.succeed(cl))(c => c.client.map(_.close()).orDie)))

  def live(host: String, port: Int): ULayer[PulsarClient] =
    live(s"pulsar://$host:$port")

  def make: ZIO[PulsarClient, PulsarClientException, JPulsarClient] =
    ZIO.environmentWithZIO[PulsarClient](_.get.client)
