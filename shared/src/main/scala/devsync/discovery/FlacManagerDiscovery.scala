/*
 * Copyright 2017 Alex Jones
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package devsync.discovery

import java.net.URL

import devsync.monads.FutureEither

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration

/**
  * A trait to allow the discovery of a Flac Manager server on a local network.
  **/
trait FlacManagerDiscovery {

  /**
    * Try and find a flac manager on the local network.
    * @param dev True if looking for a development server, false otherwise.
    * @param timeout The amount to time to search before giving up.
    * @param ec An execution context used for running future events.
    * @return A future eventually containing the root url or an error.
    */
  def discover(dev: Boolean, timeout: Duration)(implicit ec: ExecutionContext): FutureEither[Exception, URL]
}
