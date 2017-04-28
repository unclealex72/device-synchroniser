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

package devsync.scalafx.util

import java.net.URL
import java.nio.file.Path

import devsync.discovery.{ClingFlacManagerDiscovery, FlacManagerDiscovery}
import devsync.json._
import devsync.remote.{ChangesClient, ChangesClientImpl}
import devsync.scalafx.sync.{DeviceDiscoverer, DeviceDiscovererImpl}
import devsync.sync._
import org.fourthline.cling.DefaultUpnpServiceConfiguration

/**
  * Created by alex on 09/04/17
  **/
object Services {

  val jsonCodec: JsonCodec = new CirceCodec
  val isoClock: IsoClock = SystemIsoClock
  val flacManagerDiscovery: FlacManagerDiscovery = new ClingFlacManagerDiscovery(new DefaultUpnpServiceConfiguration())
  val device: Device[Path] = new DeviceImpl[Path](jsonCodec, isoClock)
  val deviceDiscoverer: DeviceDiscoverer = new DeviceDiscovererImpl(device)
  def changesClient(url: URL): ChangesClient = new ChangesClientImpl(jsonCodec, url)

}
