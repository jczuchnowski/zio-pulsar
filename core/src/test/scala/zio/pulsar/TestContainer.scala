package zio.pulsar

import com.dimafeng.testcontainers.SingleContainer
import com.dimafeng.testcontainers.PulsarContainer
import org.testcontainers.utility.DockerImageName
import zio._

object TestContainer {

  val pulsar: ZLayer[Any, Throwable, PulsarContainer] =
    ZManaged.makeEffect {
      val c = new PulsarContainer()
      c.start()
      c
    } { container => container.stop() }.toLayer

}
