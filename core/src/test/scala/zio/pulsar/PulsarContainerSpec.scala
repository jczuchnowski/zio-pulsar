package zio.pulsar

import org.apache.pulsar.client.api.PulsarClientException
import zio.Scope
import zio.test.*

import java.util.Properties
import zio.test.ZIOSpecDefault

trait PulsarContainerSpec extends ZIOSpecDefault {

  type PulsarEnvironment = TestEnvironment & PulsarClient & Scope

  val pulsarClientLayer = TestContainer
    .pulsar
    .flatMap(a =>
      PulsarClient.live(a.get.pulsarBrokerUrl())
    ).orDie

  val layer = (Scope.default ++ testEnvironment) >+> pulsarClientLayer

  override def spec =
    specLayered.provideLayerShared(layer)

  def specLayered: Spec[PulsarEnvironment, PulsarClientException]

}
