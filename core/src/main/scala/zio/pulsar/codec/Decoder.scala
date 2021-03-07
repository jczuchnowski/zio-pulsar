package zio.pulsar.codec

trait Decoder[M]:
  def decode(m: Array[Byte]): M

given stringDecoder: Decoder[String] with
  def decode(m: Array[Byte]) = new String(m)
