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
  * Created by alex on 20/03/17
  *
  * An trait that can read and parse changelogs changes.
  **/
trait JsonCodec {

  def parseChangelog(json: String): Either[Exception, Changelog]

  def parseChanges(json: String): Either[Exception, Changes]
  
  def parseTags(json: String): Either[Exception, Tags]

  def parseDeviceDescriptor(json: String): Either[Exception, DeviceDescriptor]

  def writeDeviceDescriptor(deviceDescriptor: DeviceDescriptor): String
}
