package zio.pulsar

import java.io.IOException

import org.apache.pulsar.client.api.{ SubscriptionInitialPosition, SubscriptionMode }
import zio._
import zio.blocking.Blocking
import zio.console._
import zio.pulsar.SubscriptionProperties.TopicSubscriptionProperties

object Example extends App {

  def run(args: List[String]): URIO[ZEnv, ExitCode] =
    app.provideCustomLayer(pulsarClient).useNow.exitCode

  val pulsarClient = PulsarClient.live("localhost", 6650)

  val app: ZManaged[PulsarClient with Blocking with Console, IOException, Unit] =
    for {
      _ <- putStrLn("Connect to Pulsar").toManaged_
      c <- Consumer.subscribe(
            Subscription(
              "my-subscription", 
              SubscriptionType.Exclusive(), 
              SubscriptionInitialPosition.Latest, 
              TopicSubscriptionProperties(
                List("my_topic"), 
                SubscriptionMode.Durable
              )
            )
          )
      p <- Producer.make("my-topic")
      _ <- c.receive.flatMap(msg => putStrLn(msg.getData.map(_.toChar).mkString)).toManaged_.fork
      _ <- p.send("My message".getBytes).toManaged_
      _ <- putStrLn("Finished").toManaged_
    } yield ()

}
