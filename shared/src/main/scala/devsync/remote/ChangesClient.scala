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

package devsync.remote

import java.io.OutputStream

import devsync.json._

/**
  * Created by alex on 22/03/17
  * A trait that can be used to find changes and changelogs
  **/
trait ChangesClient {

  /**
    * Get the changes for a user since a specific date.
    */
  def changesSince(user: String, maybeSince: Option[IsoDate]): Either[Exception, Changes]

  /**
    * Count the number of changelog items for a user since a specific date
    */
  def changelogSince(user: String, maybeSince: Option[IsoDate]): Either[Exception, Changelog]

  def music(item: HasLinks with HasRelativePath, out: OutputStream): Either[Exception, Unit]

  def tags(item: HasLinks with HasRelativePath): Either[Exception, Tags]

  def artwork(item: HasLinks with HasRelativePath, out: OutputStream): Either[Exception, Unit]
}
