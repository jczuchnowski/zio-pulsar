package zio.pulsar

import zio.duration.Duration
import org.apache.pulsar.client.api.{ BatchReceivePolicy, DeadLetterPolicy }

final case class ConsumerConfig(
  ackTimeout: Duration,
  ackTimeoutTickTime: Duration,
  negativeAckRedeliveryDelay: Duration,
  receiverQueueSize: Int,
  acknowledgmentGroupTime: Duration,
  replicateSubscriptionState: Boolean,
  maxTotalReceiverQueueSizeAcrossPartitions: Int,
  priorityLevel: Int,
  properties: Map[String, String],
  deadLetterPolicy: Option[DeadLetterPolicy],
  autoUpdatePartitions: Boolean,
  autoUpdatePartitionsInterval: Duration,
  batchReceivePolicy: Option[BatchReceivePolicy],
  enableRetry: Boolean,
  enableBatchIndexAcknowledgment: Boolean,
  maxPendingChuckedMessage: Int,
  autoAckOldestChunkedMessageOnQueueFull: Boolean,
  expireTimeOfIncompleteChunkedMessage: Duration
  //startMessageIdInclusive
  ///intercept
  //cryptoKeyReader
  //messageCrypto
  //cryptoFailureAction
  //consumerEventListener
)