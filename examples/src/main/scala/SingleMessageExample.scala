package examples

import zio._
import zio.clock._
import zio.console._
//import zio.logging._
import zio.pulsar._
//import zio.pulsar.SubscriptionProperties.TopicSubscriptionProperties
import org.apache.pulsar.client.api.PulsarClientException
//import org.apache.pulsar.client.api.SubscriptionMode

object SingleMessageExample extends App {

  def run(args: List[String]): URIO[ZEnv, ExitCode] =
    app.provideCustomLayer(layer).useNow.exitCode

  val pulsarClient = PulsarClient.live("localhost", 6650)

  // val logger =
  //   Logging.console(
  //     logLevel = LogLevel.Info,
  //     format = LogFormat.ColoredLogFormat()
  //   ) >>> Logging.withRootLoggerName("single-message-example")`

  val layer = ((Console.live ++ Clock.live)/* >>> logger*/) >+> pulsarClient

  val topic = "my-topic-1"

  val app: ZManaged[PulsarClient/* with Logging*/, PulsarClientException, Unit] =
    for
      //_ <- log.info("Connect to Pulsar").toManaged_
      client <- PulsarClient.make.toManaged_
      c   <- ConsumerBuilder(client)
               .withSubscription(Subscription("my-subscription", SubscriptionType.Shared))
               .withReadCompacted
               .withTopic(topic)
               .build
      p <- Producer.make(topic)
      _ <- p.send("Hello!".getBytes).toManaged_
      m <- c.receive.toManaged_
      //_ <- log.info("Received: " + m.getData.map(_.toChar).mkString).toManaged_
      //_ <- log.info("Finished").toManaged_
    yield ()

}
