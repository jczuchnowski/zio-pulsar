package zio.pulsar.json

import java.nio.charset.StandardCharsets

import org.apache.pulsar.client.api.Schema
import org.apache.pulsar.client.impl.schema.SchemaInfoImpl
import org.apache.pulsar.common.schema.{ SchemaInfo, SchemaType }
import zio.json._
import org.apache.pulsar.client.impl.schema.SchemaInfoImpl
//import zio.pulsar.codec._

given jsonSchema[T](using encoder: JsonEncoder[T], decoder: JsonDecoder[T]): Schema[T] with
  
  override def clone(): Schema[T] = this
  
  override def encode(t: T): Array[Byte] = t.toJson.getBytes(StandardCharsets.UTF_8)
  
  override def decode(bytes: Array[Byte]): T = 
    new String(bytes, StandardCharsets.UTF_8)
      .fromJson[T]
      .fold(s => throw new RuntimeException(s), identity)
  
  override def getSchemaInfo: SchemaInfo =
    SchemaInfoImpl.builder
      .name(manifest[T].runtimeClass.getCanonicalName)
      .`type`(SchemaType.JSON)
      .schema("""{"type":"any"}""".getBytes("UTF-8"))
      .build
      