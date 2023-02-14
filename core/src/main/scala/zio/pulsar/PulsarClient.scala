package zio.pulsar

import org.apache.pulsar.client.api.{ PulsarClient => JPulsarClient, PulsarClientException }
import zio._

trait PulsarClient:
  def client: IO[PulsarClientException, JPulsarClient]
end PulsarClient

object PulsarClient:

  def live(url: String): URLayer[Scope, PulsarClient] =
    val builder = JPulsarClient.builder().serviceUrl(url)

    val cl = new PulsarClient {
      val client = ZIO.attempt(builder.build).refineToOrDie[PulsarClientException]
    }

    ZLayer(ZIO.acquireRelease(ZIO.succeed(cl))(c => c.client.map(_.close()).orDie))
  end live

  def live(host: String, port: Int): URLayer[Scope, PulsarClient] =
    live(s"pulsar://$host:$port")

  def make: ZIO[PulsarClient, PulsarClientException, JPulsarClient] =
    ZIO.environmentWithZIO[PulsarClient](_.get.client)
