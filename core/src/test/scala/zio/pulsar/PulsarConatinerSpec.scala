package zio.pulsar

import zio.test._
import java.util.Properties

trait PulsarContainerSpec extends DefaultRunnableSpec {

  type PulsarEnvironment = TestEnvironment & PulsarClient

  val pulsarClientLayer = TestContainer
    .pulsar
    .flatMap(a =>
      PulsarClient.live(a.get.pulsarBrokerUrl())
    ).orDie

  val layer = TestEnvironment.live >+> pulsarClientLayer

  override def spec: Spec[TestEnvironment, TestFailure[Any], TestSuccess] =
    specLayered.provideCustomLayerShared(layer)

  def specLayered: Spec[PulsarEnvironment, TestFailure[Object], TestSuccess]

}
