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
  * An trait that can read and parse [[Changelog]]s, [[Changes]] and [[DeviceDescriptor]]s.
  **/
trait JsonCodec {

  /**
    * Parse a [[Changelog]].
    * @param json The JSON to parse.
    * @return A new [[Changelog]] or an error.
    */
  def parseChangelog(json: String): Either[Exception, Changelog]

  /**
    * Parse [[Changes]].
    * @param json The JSON to parse.
    * @return A new [[Changes]] or an error.
    */
  def parseChanges(json: String): Either[Exception, Changes]

  /**
    * Parse [[Tags]].
    * @param json The JSON to parse.
    * @return A new [[Tags]] or an error.
    */
  def parseTags(json: String): Either[Exception, Tags]

  /**
    * Parse a [[DeviceDescriptor]].
    * @param json The JSON to parse.
    * @return A new [[DeviceDescriptor]] or an error.
    */

  def parseDeviceDescriptor(json: String): Either[Exception, DeviceDescriptor]

  /**
    * Convert a [[DeviceDescriptor]] to JSON.
    * @param deviceDescriptor The [[DeviceDescriptor]] to write.
    * @return A string containing a JSON object.
    */
  def writeDeviceDescriptor(deviceDescriptor: DeviceDescriptor): String
}
