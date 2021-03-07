package zio.pulsar

import org.apache.pulsar.client.api.{
  Message => JMessage,
  MessageId,
}
import zio.pulsar.codec.Decoder

final case class Message[M](id: MessageId, value: M)

object Message:
  def from[M](m: JMessage[Array[Byte]])(using decoder: Decoder[M]): Message[M] =
    Message(m.getMessageId, decoder.decode(m.getValue))
