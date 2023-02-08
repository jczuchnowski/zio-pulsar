package zio.pulsar.json

import java.nio.charset.StandardCharsets

import com.sksamuel.avro4s.{ AvroSchema, SchemaFor }
import org.apache.pulsar.client.api.Schema
import org.apache.pulsar.client.impl.schema.{ JSONSchema, SchemaInfoImpl }
import org.apache.pulsar.common.schema.{ SchemaInfo, SchemaType }
import zio.json._

object Schema:
  def jsonSchema[T](using codec: JsonCodec[T], avroSchema: SchemaFor[T], manifest: Manifest[T]): Schema[T] =
    new Schema[T] {

      override def clone(): Schema[T] = this

      override def encode(t: T): Array[Byte] = t.toJson.getBytes(StandardCharsets.UTF_8)

      override def decode(bytes: Array[Byte]): T =
        new String(bytes, StandardCharsets.UTF_8)
          .fromJson[T]
          .fold(s => throw new RuntimeException(s), identity)

      val s = AvroSchema[T](using avroSchema)

      override def getSchemaInfo: SchemaInfo =
        SchemaInfoImpl.builder
          .name(manifest.runtimeClass.getCanonicalName)
          .`type`(SchemaType.JSON)
          .schema(s.toString.getBytes("UTF-8"))
          .build
    }
