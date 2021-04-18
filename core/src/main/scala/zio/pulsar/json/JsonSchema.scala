package zio.pulsar.json

import java.nio.charset.StandardCharsets

import org.apache.pulsar.client.api.Schema
import org.apache.pulsar.common.schema.{ SchemaInfo, SchemaType }
import zio.json._
import zio.pulsar.codec._

given jsonSchema[T: Manifest](using encoder: JsonEncoder[T], decoder: JsonDecoder[T]): Schema[T] with
  
  def clone(): Schema[T] = this
  
  override def encode(t: T): Array[Byte] = t.toJson.getBytes(StandardCharsets.UTF_8)
  
  override def decode(bytes: Array[Byte]): T = 
    new String(bytes, StandardCharsets.UTF_8)
      .fromJson[T]
      .fold(s => throw new RuntimeException(s), identity)
  
  override def getSchemaInfo: SchemaInfo =
    new SchemaInfo()
      .setName(manifest[T].runtimeClass.getCanonicalName)
      .setType(SchemaType.JSON)
      .setSchema("""{"type":"any"}""".getBytes("UTF-8"))
      