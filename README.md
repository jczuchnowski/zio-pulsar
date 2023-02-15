# ZIO Pulsar

| CI | Release | Snapshot |
| --- | --- | --- |
| ![CI][Badge-CI] | [![Release Artifacts][badge-releases]][link-releases] | [![Snapshot Artifacts][badge-snapshots]][link-snapshots] |

Purely functional Scala wrapper over the official Pulsar client.

- Type-safe (utilizes Scala type system to reduce runtime exceptions present in the official Java client)
- Streaming-enabled (naturally integrates with ZIO Streams)
- ZIO integrated (uses common ZIO primitives like ZIO effect and ZManaged to reduce the boilerplate and increase expressiveness)

## Compatibility

ZIO Pulsar is a Scala 3 library, so it's compatible with Scala 3 applications as well as Scala 2.13.6+ (see [forward compatibility](https://www.scala-lang.org/blog/2020/11/19/scala-3-forward-compat.html) for more information.

## Getting started

Add the following dependency to your `build.sbt` file:

Scala 3
```
libraryDependencies += "com.github.jczuchnowski" %% "zio-pulsar" % zioPulsarVersion
```

Scala 2.13.6+ (sbt 1.5.x)
```
libraryDependencies += 
  ("com.github.jczuchnowski" %% "zio-pulsar" % zioPulsarVersion).cross(CrossVersion.for2_13Use3)
```

ZIO Pulsar also needs ZIO and ZIO Streams to be provided:

```
libraryDependencies ++= Seq(
  "dev.zio" %% "zio"         % zioVersion,
  "dev.zio" %% "zio-streams" % zioVersion
)
```

Simple example of consumer and producer:

```scala
object SingleMessageExample extends ZIOAppDefault:

  val pulsarClient = PulsarClient.live("localhost", 6650)

  val topic = "single-topic"

  val app: ZIO[PulsarClient & Scope, PulsarClientException, Unit] =
    for
      builder  <- ConsumerBuilder.make(JSchema.STRING)
      consumer <- builder
                    .topic(topic)
                    .subscription(Subscription("my-subscription", SubscriptionType.Shared))
                    .build
      producer <- Producer.make(topic, JSchema.STRING)
      _        <- producer.send("Hello!")
      m        <- consumer.receive
      _ = println(m.getValue)
    yield ()

  override def run = app.provideLayer(pulsarClient ++ Scope.default).exitCode

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
[badge-releases]: https://img.shields.io/nexus/r/https/oss.sonatype.org/com.github.jczuchnowski/zio-pulsar_3 "Sonatype Releases"
[badge-snapshots]: https://img.shields.io/nexus/s/https/oss.sonatype.org/com.github.jczuchnowski/zio-pulsar_3 "Sonatype Snapshots"
[link-releases]: https://oss.sonatype.org/content/repositories/releases/com/github/jczuchnowski/zio-pulsar_3/ "Sonatype Releases"
[link-snapshots]: https://oss.sonatype.org/content/repositories/snapshots/com/github/jczuchnowski/zio-pulsar_3/ "Sonatype Snapshots"
