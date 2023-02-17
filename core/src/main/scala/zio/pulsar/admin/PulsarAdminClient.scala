package zio.pulsar.admin

import org.apache.pulsar.client.admin.{ PulsarAdmin as JPulsarAdmin, PulsarAdminBuilder as JAdminBuilder }
import org.apache.pulsar.client.api.{ Authentication, PulsarClientException }
import zio.pulsar.*
import zio.*
import zio.pulsar.admin.AdminConfigPart.{ ConfigComplete, Empty, ServiceUrl }

import scala.jdk.CollectionConverters.*

/**
 * @author
 *   梦境迷离
 * @version 1.0,2023/2/17
 */

sealed trait AdminConfigPart
object AdminConfigPart:
  sealed trait Empty      extends AdminConfigPart
  sealed trait ServiceUrl extends AdminConfigPart

  type ConfigComplete = Empty with ServiceUrl
end AdminConfigPart

final class PulsarAdminClient[S <: AdminConfigPart] private (adminBuilder: JAdminBuilder):

  def serviceHttpUrl(serviceHttpUrl: String): PulsarAdminClient[S with ServiceUrl] =
    new PulsarAdminClient(adminBuilder.serviceHttpUrl(serviceHttpUrl))

  def authentication(authPluginClassName: String, authParamsString: String): PulsarAdminClient[S] =
    new PulsarAdminClient(adminBuilder.authentication(authPluginClassName, authParamsString))

  def authentication(authPluginClassName: String, authParams: Map[String, String]): PulsarAdminClient[S] =
    new PulsarAdminClient(adminBuilder.authentication(authPluginClassName, authParams.asJava))

  def authentication(authentication: Authentication): PulsarAdminClient[S] =
    new PulsarAdminClient(adminBuilder.authentication(authentication))

  def build(implicit en: S =:= ConfigComplete): ZIO[Scope, PulsarClientException, JPulsarAdmin] =
    val client = ZIO.attempt(adminBuilder.build()).refineToOrDie[PulsarClientException]
    ZIO.acquireRelease(client)(client => ZIO.attempt(client.close()).orDie)

end PulsarAdminClient

object PulsarAdminClient:
  def make(serviceHttpUrl: String): Task[PulsarAdminClient[ConfigComplete]] =
    ZIO.attempt(new PulsarAdminClient(JPulsarAdmin.builder().serviceHttpUrl(serviceHttpUrl)))

  lazy val make: Task[PulsarAdminClient[Empty]] =
    ZIO.attempt(new PulsarAdminClient(JPulsarAdmin.builder()))
