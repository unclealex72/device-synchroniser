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

package uk.co.unclealex.devsync

import java.net.URL

import android.support.v4.provider.DocumentFile
import devsync.discovery.{ClingFlacManagerDiscovery, FlacManagerDiscovery}
import devsync.json.{CirceCodec, IsoClock, JsonCodec, SystemIsoClock}
import devsync.remote.{ChangesClient, ChangesClientImpl}
import devsync.sync.{Device, DeviceImpl}
import org.fourthline.cling.android.AndroidUpnpServiceConfiguration

/**
  * Created by alex on 26/03/17
  * An object that holds singleton services
  **/
object Services {

  val jsonCodec: JsonCodec = new CirceCodec

  val flacManagerDiscovery: FlacManagerDiscovery = new ClingFlacManagerDiscovery(new AndroidUpnpServiceConfiguration)

  def changesClient(baseUrl: URL): ChangesClient = new ChangesClientImpl(jsonCodec, baseUrl)

  val isoClock: IsoClock = SystemIsoClock

  val device: Device[DocumentFile] = new DeviceImpl[DocumentFile](jsonCodec, isoClock)

}
