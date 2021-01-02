package examples

//import java.io.IOException

import zio._
import zio.blocking.Blocking
import zio.clock._
import zio.console._
import zio.pulsar._
import zio.pulsar.SubscriptionProperties.TopicSubscriptionProperties
import zio.logging._

object Example2 extends App {

  def run(args: List[String]): URIO[ZEnv, ExitCode] =
    app.provideCustomLayer(layer).useNow.exitCode

  val pulsarClient = PulsarClient.live("localhost", 6650)

  val logger =
    Logging.console(
      logLevel = LogLevel.Info,
      format = LogFormat.ColoredLogFormat()
    ) >>> Logging.withRootLoggerName("my-component")

  val layer = logger ++ (((Console.live ++ Clock.live) >>> logger) >>> pulsarClient)

  val app: ZManaged[PulsarClient with Blocking with Console with Logging, Throwable, Unit] =
    for {
      _ <- putStrLn("Connect to Pulsar").toManaged_
      c <- Consumer.streaming(
            Subscription(
              name = "my-subscription", 
              `type` = Some(SubscriptionType.Exclusive()),
              properties = TopicSubscriptionProperties(
                List("my_topic")
              )
            )
          )
      _ <- c.receive.foreach(a => ZIO.effect(c.consumer.acknowledge(a.getMessageId())) *> putStrLn(a.getMessageId.toString) *> putStrLn(a.getData().map(_.toChar).mkString)).toManaged_
      _ <- putStrLn("Finished").toManaged_
    } yield ()

}
