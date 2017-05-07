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

package devsync.scalafx

import java.net.URL
import java.nio.file.Path

import devsync.discovery.{ClingFlacManagerDiscovery, FlacManagerDiscovery}
import devsync.json._
import devsync.remote.{ChangesClient, ChangesClientImpl}
import devsync.sync._
import org.fourthline.cling.DefaultUpnpServiceConfiguration

/**
  * Services used throughout device synchronisation.
  **/
object Services {

  /**
    * The default [[JsonCodec]] to use.
    */
  val jsonCodec: JsonCodec = new CirceCodec

  /**
    * The default [[IsoClock]] to use.
    */
  val isoClock: IsoClock = SystemIsoClock

  /**
    * The default [[FlacManagerDiscovery]] to use.
    */
  val flacManagerDiscovery: FlacManagerDiscovery = new ClingFlacManagerDiscovery(new DefaultUpnpServiceConfiguration())

  /**
    * The default [[Device]] to use.
    */
  val device: Device[Path] = new DeviceImpl[Path](jsonCodec, isoClock)

  /**
    * The default [[DeviceDiscoverer]] to use.
    */
  val deviceDiscoverer: DeviceDiscoverer = new DeviceDiscovererImpl(device)

  /**
    * Create a [[ChangesClient]].
    * @param url The URL of the Flac Manager server.
    * @return A new [[ChangesClient]] that looks for changes at the given URL.
    */
  def changesClient(url: URL): ChangesClient = new ChangesClientImpl(jsonCodec, url)

}
