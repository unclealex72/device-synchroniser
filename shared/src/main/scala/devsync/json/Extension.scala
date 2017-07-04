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

import enumeratum._

import scala.collection.immutable

/**
  * An extension that is used to determine the type of files that are stored on the device.
  */
sealed trait Extension extends EnumEntry {

  /**
    * The file extension.
    * @return The file extension.
    */
  val extension: String

  /**
    * The mime type
    * @return The mime type.
    */
  val mimeType: String
}

/**
  * A holder object for the different extension types.
  */
object Extension extends Enum[Extension] {

  private[Extension] case class ExtensionImpl(extension: String, mimeType: String)

  /**
    * MP3 files.
    */
  object MP3 extends ExtensionImpl("mp3", "audio/mpeg") with Extension

  /**
    * M4A files.
    */
  object M4A extends ExtensionImpl("m4a", "audio/m4a") with Extension

  val values: immutable.IndexedSeq[Extension] = findValues
}