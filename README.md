# zio-pulsar

Purely functional Scala interface to Apache Pulsar.

Main features:
- Type-safe (utilizes Scala type system to reduce runtime exceptions present in the official Java client)
- Streaming-enabled (naturally integrates with ZIO Streams)
- ZIO integrated (uses common ZIO primitives like ZIO effect and ZManaged to reduce the boilerplate and increase expressiveness)
