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
  * Created by alex on 02/04/17
  **/
object IntentHelper {

  implicit class IntentHelperImplicits(intent: Intent) {

    def put(deviceDescriptor: DeviceDescriptor, resourceUri: String, serverUrl: String): Unit = {
      intent.putExtra(Constants.Intent.deviceDescriptor, deviceDescriptor)
      intent.putExtra(Constants.Intent.resourceUri, resourceUri)
      intent.putExtra(Constants.Intent.serverUrl, serverUrl)
    }

    def deviceDescriptor: DeviceDescriptor = {
      intent.getExtras.getSerializable(Constants.Intent.deviceDescriptor).asInstanceOf[DeviceDescriptor]
    }

    def resourceUri: String = {
      intent.getExtras.getString(Constants.Intent.resourceUri)
    }

    def serverUrl: String = {
      intent.getExtras.getString(Constants.Intent.serverUrl)
    }
  }
}
