package examples

//import java.io.IOException

import zio._
import zio.blocking.Blocking
import zio.clock._
import zio.console._
import zio.pulsar._
import zio.pulsar.SubscriptionProperties.TopicSubscriptionProperties
import zio.logging._

object Example extends App {

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
      c <- Consumer.subscribe(
            Subscription(
              name = "my-subscription", 
              `type` = Some(SubscriptionType.Exclusive()),
              properties = TopicSubscriptionProperties(
                List("my_topic")
              )
            )
          )
      _ <- Consumer.subscribe(
            Subscription(
              name = "my-subscription", 
              `type` = Some(SubscriptionType.Failover()),
              properties = TopicSubscriptionProperties(
                List("my-topic")
              )
            )
          )
      p <- Producer.make("my_topic")
      f <- c.receive.flatMap(msg => putStrLn(msg.getData.map(_.toChar).mkString)).toManaged_.fork
      _ <- p.send("My message".getBytes).repeatN(10).toManaged_
      _ <- f.join.toManaged_
      _ <- putStrLn("Finished").toManaged_
    } yield ()

}
