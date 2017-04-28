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

package devsync.json

/**
  * Created by alex on 24/03/17
  * A class that defines the JSON that is stored on a device to find out if it needs to be serialised.
  **/
case class DeviceDescriptor(user: String, maybeLastModified: Option[IsoDate], maybeOffset: Option[Int]) extends Serializable {

  def withLastModified(lastModified: IsoDate): DeviceDescriptor = this.copy(maybeLastModified = Some(lastModified))

  def withOffset(offset: Int): DeviceDescriptor = this.copy(maybeOffset = Some(offset))
}
