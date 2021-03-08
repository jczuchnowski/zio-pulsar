package zio.pulsar.codec

trait Encoder[M]:
  def encode(m: M): Array[Byte]

given stringEncoder: Encoder[String] with
  def encode(m: String) = m.getBytes
