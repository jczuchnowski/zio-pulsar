# ZIO Pulsar

| CI | Release | Snapshot |
| --- | --- | --- |
| ![CI][Badge-CI] | [![Release Artifacts][badge-releases]][link-releases] | [![Snapshot Artifacts][badge-snapshots]][link-snapshots] |

## Overview

Purely functional Scala wrapper over the official Pulsar client.

- Type-safe (utilizes Scala type system to reduce runtime exceptions present in the official Java client)
- Streaming-enabled (naturally integrates with ZIO Streams)
- ZIO integrated (uses common ZIO primitives like ZIO effect and ZManaged to reduce the boilerplate and increase expressiveness)

## Running locally

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
[badge-releases]: https://img.shields.io/nexus/r/https/org.github.jczuchnowski/zio-pulsar_3.0.0-RC1.svg "Sonatype Releases"
[badge-snapshots]: https://img.shields.io/nexus/s/https/org.github.jczuchnowski/zio-pulsar_3.0.0-RC1.svg "Sonatype Snapshots"
[link-releases]: https://oss.sonatype.org/content/repositories/releases/com/github/jczuchnowski/zio-pulsar_3.0.0-RC1/ "Sonatype Releases"
[link-snapshots]: https://oss.sonatype.org/content/repositories/snapshots/com/github/jczuchnowski/zio-pulsar_3.0.0-RC1/ "Sonatype Snapshots"
