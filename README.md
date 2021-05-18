# ZIO Pulsar

| CI | Release | Snapshot |
| --- | --- | --- |
| ![CI][Badge-CI] | [![Release Artifacts][badge-releases]][link-releases] | [![Snapshot Artifacts][badge-snapshots]][link-snapshots] |

Purely functional Scala wrapper over the official Pulsar client.

- Type-safe (utilizes Scala type system to reduce runtime exceptions present in the official Java client)
- Streaming-enabled (naturally integrates with ZIO Streams)
- ZIO integrated (uses common ZIO primitives like ZIO effect and ZManaged to reduce the boilerplate and increase expressiveness)

## Getting started

⚠️ ZIO Pulsar is written in Scala 3 and will work with Scala 3, but the current Scala 3 release (3.0.0-RC3) uses TASTy format in version incompatible with any Scala 2.13 realease. So you will need to wait for a new Scala 2.13 to be able to use the [forward compatibility](https://www.scala-lang.org/blog/2020/11/19/scala-3-forward-compat.html) feature.

Add the following dependency to your `build.sbt` file:

```
libraryDependencies += "com.github.jczuchnowski" %% "zio-pulsar" % "<version>"
```

ZIO Pulsar depents on ZIO and ZIO Streams, so make sure you have these dependencies, because ZIO Pulsar will not pull them itself:

```
libraryDependencies ++= Seq(
  "dev.zio" %% "zio"         % zioVersion,
  "dev.zio" %% "zio-streams" % zioVersion
)
```

Simple example of consumer and producer:

```scala
import org.apache.pulsar.client.api.{ PulsarClientException, Schema }
import zio._
import zio.pulsar._

object Main extends App:

  val pulsarClient = PulsarClient.live("localhost", 6650)

  val topic = "my-topic"

  val app: ZManaged[PulsarClient, PulsarClientException, Unit] =
    for
      builder  <- ConsumerBuilder.make(Schema.STRING).toManaged_
      consumer <- builder
                    .topic(topic)
                    .subscription(
                      Subscription(
                        "my-subscription", 
                        SubscriptionType.Shared))
                    .build
      producer <- Producer.make(topic, Schema.STRING)
      _        <- producer.send("Hello!").toManaged_
      m        <- consumer.receive.toManaged_
    yield ()
    
  def run(args: List[String]): URIO[ZEnv, ExitCode] =
    app.provideCustomLayer(pulsarClient).useNow.exitCode
```

## Running examples locally

To try the examples from the `examples` subproject you'll need a Pulsar instance running locally. You can set one up using docker:
```
docker run -it \
  -p 6650:6650 \
  -p 8080:8080 \
  --mount source=pulsardata,target=/pulsar/data \
  --mount source=pulsarconf,target=/pulsar/conf \
  --network pulsar \
  apachepulsar/pulsar:2.7.0 \
  bin/pulsar standalone
```

[Badge-CI]: https://github.com/jczuchnowski/zio-pulsar/actions/workflows/scala.yml/badge.svg
[badge-releases]: https://img.shields.io/nexus/r/https/oss.sonatype.org/io.scalac/mesmer-akka-extension_2.13 "Sonatype Releases"
[badge-snapshots]: https://img.shields.io/nexus/s/https/oss.sonatype.org/io.scalac/mesmer-akka-extension_2.13 "Sonatype Snapshots"
[link-releases]: https://oss.sonatype.org/content/repositories/releases/io/scalac/mesmer-akka-extension_2.13/ "Sonatype Releases"
[link-snapshots]: https://oss.sonatype.org/content/repositories/snapshots/io/scalac/mesmer-akka-extension_2.13/ "Sonatype Snapshots"
