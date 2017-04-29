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

import android.content.Intent
import devsync.json.DeviceDescriptor

/**
  * Implicits to add a level of strong typing to intents.
  **/
object IntentHelper {

  /**
    * Implicits to add a level of strong typing to intents.
    * @param intent The intent to extend.
    **/
  implicit class IntentHelperImplicits(intent: Intent) {

    /**
      * Add a device descriptor, device descriptor resource URI and a Flac Manager server URL to an intent.
      * @param deviceDescriptor The device descriptor to add.
      * @param resourceUri The URI of the device descriptor.
      * @param serverUrl The URL of the Flac Manager server.
      */
    def put(deviceDescriptor: DeviceDescriptor, resourceUri: String, serverUrl: String): Unit = {
      intent.putExtra(Constants.Intent.deviceDescriptor, deviceDescriptor)
      intent.putExtra(Constants.Intent.resourceUri, resourceUri)
      intent.putExtra(Constants.Intent.serverUrl, serverUrl)
    }

    /**
      * Get a device descriptor from an intent.
      * @return A device descriptor.
      */
    def deviceDescriptor: DeviceDescriptor = {
      intent.getExtras.getSerializable(Constants.Intent.deviceDescriptor).asInstanceOf[DeviceDescriptor]
    }

    /**
      * Get the resource URI of a device descriptor from an intent.
      * @return A resource URI of a device descriptor.
      */
    def resourceUri: String = {
      intent.getExtras.getString(Constants.Intent.resourceUri)
    }

    /**
      * Get the Flac Manager server URL from an intent.
      * @return A Flac Manager server URL.
      */
    def serverUrl: String = {
      intent.getExtras.getString(Constants.Intent.serverUrl)
    }
  }
}
