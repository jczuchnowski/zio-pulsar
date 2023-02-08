import com.sksamuel.avro4s._
import org.apache.pulsar.client.impl.schema.JSONSchema
import org.apache.pulsar.client.api.schema.SchemaDefinition
import org.apache.pulsar.client.api.schema.SchemaReader
import java.io.InputStream
import java.nio.charset.Charset
import org.apache.pulsar.client.api.schema.SchemaWriter
import zio.json._

case class User(email: String, name: Option[String], age: Int)

@main
def schemaExample2 =
  val schema = AvroSchema[User]
  println(schema.toString)
  // {"type":"record","name":"User","fields":[{"name":"name","type":"string"},{"name":"age","type":"int"}]}

  // val jSchema = JSONSchema.of(classOf[User])
  // println(jSchema.getAvroSchema.toString)
  // {"type":"record","name":"User","fields":[{"name":"age","type":"int"},{"name":"name","type":["null","string"],"default":null}]}

  val cSchema: SchemaDefinition[User] = SchemaDefinition
    .builder[User]()
    .withPojo(classOf[User])
    .withAlwaysAllowNull(false)
    .withSchemaReader(new JSchemaReader())
    .withSchemaWriter(new JSchemaWriter())
    .withSupportSchemaVersioning(true)
    .build()

  println(JSONSchema.of(cSchema).getAvroSchema.toString)

// custom Pulsar SchemaReader
class JSchemaReader extends SchemaReader[User] {
  implicit val decoder: JsonDecoder[User] = DeriveJsonDecoder.gen[User]

  override def read(bytes: Array[Byte], offset: Int, length: Int): User =
    new String(bytes, offset, length).fromJson[User].getOrElse(throw new RuntimeException("test"))

  override def read(inputStream: InputStream): User =
    new String(inputStream.readAllBytes(), Charset.defaultCharset())
      .fromJson[User]
      .getOrElse(throw new RuntimeException("test"))
}

// custom Pulsar SchemaWriter
class JSchemaWriter extends SchemaWriter[User] {
  implicit val encoder: JsonEncoder[User]        = DeriveJsonEncoder.gen[User]
  override def write(message: User): Array[Byte] =
    message.toJson.getBytes()
}
