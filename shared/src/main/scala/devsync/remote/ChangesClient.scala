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
import org.threeten.bp.Instant

/**
  * A trait that can be used to find [[Changes]] and [[Changelog]]s from a Flac Manager server.
  **/
trait ChangesClient {

  /**
    * Get the changes for a user since a specific date.
    * @param user The user to search for.
    * @param maybeSince The earliest time for changes or none for all changes.
    * @return Either a [[Changes]] object from the server or an exception.
    */
  def changesSince(user: String, maybeSince: Option[Instant]): Either[Exception, Changes]

  /**
    * Get the changelog for a user since a specific date..
    * @param user The user to search for.
    * @param maybeSince The earliest time for changes or none for all changes.
    * @return Either a [[Changes]] object from the server or an exception.
    */
  def changelogSince(user: String, maybeSince: Option[Instant]): Either[Exception, Changelog]

  /**
    * Download the music for a track.
    * @param item The item who's music needs to be downloaded.
    * @param out An output stream to where the music will be copied.
    * @return Either [[Unit]] or an exception.
    */
  def music(item: HasLinks with HasRelativePath, out: OutputStream): Either[Exception, Unit]

  /**
    * Download the tags for a track.
    * @param item The item who's tags needs to be downloaded.
    * @return Either the item's [[Tags]] or an exception.
    */
  def tags(item: HasLinks with HasRelativePath): Either[Exception, Tags]

  /**
    * Download the album artwork for a track.
    * @param item The item who's album artwork needs to be downloaded.
    * @param out An output stream to where the album artwork will be copied.
    * @return Either [[Unit]] or an exception.
    */
  def artwork(item: HasLinks with HasRelativePath, out: OutputStream): Either[Exception, Unit]
}
