package zio.pulsar

import org.apache.pulsar.client.api.interceptor.ProducerInterceptor
import org.apache.pulsar.client.api.{
  CompressionType,
  MessageRoutingMode,
  ProducerAccessMode,
  ProducerBuilder as JProducerBuilder,
  PulsarClientException,
  Schema
}
import zio.pulsar.ProducerConfigPart.*
import zio.pulsar.Properties.*
import zio.pulsar.Properties.StringProperties
import zio.pulsar.Property.StringProperty
import zio.{ Scope, ZIO }

import scala.jdk.CollectionConverters.*
import java.util.concurrent.TimeUnit

/**
 * @author
 *   梦境迷离
 * @version 1.0,2023/2/14
 */
object ProducerConfigPart:
  sealed trait Empty   extends ProducerConfigPart
  sealed trait ToTopic extends ProducerConfigPart

  type ConfigComplete = Empty with ToTopic
end ProducerConfigPart

sealed trait ProducerConfigPart

final class ProducerBuilder[T, S <: ProducerConfigPart] private (
  builder: JProducerBuilder[T]
):
  def topic(topic: String): ProducerBuilder[T, S with ToTopic] =
    new ProducerBuilder(builder.topic(topic))

  def productName(productName: String): ProducerBuilder[T, S] =
    new ProducerBuilder(builder.producerName(productName))

  def messageRoutingMode(messageRoutingMode: MessageRoutingMode): ProducerBuilder[T, S] =
    new ProducerBuilder(builder.messageRoutingMode(messageRoutingMode))

  def sendTimeout(sendTimeout: Int, unit: TimeUnit): ProducerBuilder[T, S] =
    new ProducerBuilder(builder.sendTimeout(sendTimeout, unit))

  def accessMode(accessMode: ProducerAccessMode): ProducerBuilder[T, S] =
    new ProducerBuilder(builder.accessMode(accessMode))

  def compressionType(compressionType: CompressionType): ProducerBuilder[T, S] =
    new ProducerBuilder(builder.compressionType(compressionType))

  def blockIfQueueFull(blockIfQueueFull: Boolean): ProducerBuilder[T, S] =
    new ProducerBuilder(builder.blockIfQueueFull(blockIfQueueFull))

  def loadConf(config: Property.Producer[_], configs: Property.Producer[_]*): ProducerBuilder[T, S] =
    new ProducerBuilder(builder.loadConf(ProducerProperties(config, configs.toList).getConfig.asJava))

  def properties(property: StringProperty, properties: StringProperty*): ProducerBuilder[T, S] =
    new ProducerBuilder(builder.properties(StringProperties(property, properties.toList).getProperties.asJava))

  def maxPendingMessages(maxPendingMessages: Int): ProducerBuilder[T, S] =
    new ProducerBuilder(builder.maxPendingMessages(maxPendingMessages))

  def batchingMaxPublishDelay(batchDelay: Long, timeUnit: TimeUnit): ProducerBuilder[T, S] =
    new ProducerBuilder(builder.batchingMaxPublishDelay(batchDelay, timeUnit))

  def batchingMaxMessages(batchMessagesMaxMessagesPerBatch: Int): ProducerBuilder[T, S] =
    new ProducerBuilder(builder.batchingMaxMessages(batchMessagesMaxMessagesPerBatch))

  def enableBatching(enableBatching: Boolean): ProducerBuilder[T, S] =
    new ProducerBuilder(builder.enableBatching(enableBatching))

  def intercept(interceptor: ProducerInterceptor, interceptors: ProducerInterceptor*): ProducerBuilder[T, S] =
    new ProducerBuilder(builder.intercept(Seq(interceptor) ++ interceptors: _*))

  def build(implicit
    ev: S =:= ConfigComplete
  ): ZIO[PulsarClient with Scope, PulsarClientException, Producer[T]] =
    val producer = ZIO.attempt(new Producer(builder.create())).refineToOrDie[PulsarClientException]
    ZIO.acquireRelease(producer)(p => ZIO.attempt(p.producer.close()).orDie)

end ProducerBuilder

object ProducerBuilder:

  lazy val make: ZIO[PulsarClient, PulsarClientException, ProducerBuilder[Array[Byte], ProducerConfigPart.Empty]] =
    ZIO.environmentWithZIO[PulsarClient](_.get.client).map(c => new ProducerBuilder(c.newProducer()))

  def make[M](
    schema: Schema[M]
  ): ZIO[PulsarClient, PulsarClientException, ProducerBuilder[M, ProducerConfigPart.Empty]] =
    ZIO.environmentWithZIO[PulsarClient](_.get.client).map(c => new ProducerBuilder(c.newProducer(schema)))
