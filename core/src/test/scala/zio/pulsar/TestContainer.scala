package zio.pulsar

import com.dimafeng.testcontainers.SingleContainer
import com.dimafeng.testcontainers.PulsarContainer
import org.testcontainers.utility.DockerImageName
import zio._

object TestContainer {

  lazy val pulsar: ZLayer[Scope, Throwable, PulsarContainer] =
    ZLayer(ZIO.acquireRelease {
      val c = new PulsarContainer("2.8.1")
      ZIO.attempt(c.start()).as(c)
    }(container => ZIO.succeed(container.stop())))

}
