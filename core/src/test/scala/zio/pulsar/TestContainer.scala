package zio.pulsar

import com.dimafeng.testcontainers.SingleContainer
import com.dimafeng.testcontainers.PulsarContainer
import org.testcontainers.utility.DockerImageName
import zio._

object TestContainer {

  val pulsar: ZLayer[Any & Scope, Throwable, PulsarContainer] =
    ZLayer(ZIO.acquireRelease {
      val c = new PulsarContainer()
      c.start()
      ZIO.succeed(c)
    }(container => ZIO.succeed(container.stop())))

}
