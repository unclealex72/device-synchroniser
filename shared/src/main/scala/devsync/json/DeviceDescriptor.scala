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

import devsync.json.Extension.MP3
import org.threeten.bp.Instant

/**
  * A class that defines the JSON that is stored on a device to find out if it needs to be serialised.
  *
  * @param user The owner of the device.
  * @param extension The type of files stored on the device.
  * @param maybeLastModified The last time the device was last synchronised, if any.
  * @param maybeOffset The offset of the change that failed last time synchronisation was attempted, if any.
  **/
case class DeviceDescriptor(
                             user: String,
                             extension: Extension,
                             maybeLastModified: Option[Instant],
                             maybeOffset: Option[Int]) extends Serializable {

  /**
    * Update the last modified time.
    * @param lastModified The new last modified time.
    * @return A new [[DeviceDescriptor]] with the given last modified time.
    */
  def withLastModified(lastModified: Instant): DeviceDescriptor = this.copy(maybeLastModified = Some(lastModified))

  /**
    * Update the offset.
    * @param offset The new last modified time.
    * @return A new [[DeviceDescriptor]] with the given offset.
    */
  def withOffset(offset: Int): DeviceDescriptor = this.copy(maybeOffset = Some(offset))
}

/**
  * Used to create device descriptors, allowing the extension parameter to be optional, defaulting to MP3.
  */
object DeviceDescriptor {

  /**
    * Create a new device descriptor.
    * @param user The owner of the device.
    * @param maybeExtension The type of files stored on the device, if any.
    * @param maybeLastModified The last time the device was last synchronised, if any.
    * @param maybeOffset The offset of the change that failed last time synchronisation was attempted, if any.
    * @return A new device descriptor whose extension will be [[devsync.json.Extension.MP3]] if not specified.
    */
  def optionalExtension(
             user: String,
             maybeExtension: Option[Extension],
             maybeLastModified: Option[Instant],
             maybeOffset: Option[Int]): DeviceDescriptor = {
    DeviceDescriptor(user, maybeExtension.getOrElse(MP3), maybeLastModified, maybeOffset)
  }
}