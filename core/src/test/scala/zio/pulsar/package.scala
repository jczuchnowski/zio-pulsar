package zio

import zio.test.TestEnvironment

/**
 * @author
 *   梦境迷离
 * @version 1.0,2023/2/16
 */
package object pulsar {
  type PulsarEnvironment = TestEnvironment with PulsarClient with Scope
}
