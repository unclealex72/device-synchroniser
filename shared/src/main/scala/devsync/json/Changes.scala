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

import org.threeten.bp.Instant

/**
  * A sequence of changes that need to be made to a device to keep it up to date with music on the Flac Manager
  * server.
 *
  * @param changes The changes that need to happen.
  **/
case class Changes(changes: Seq[Change])

/**
  * A single change that can be either an addition or removal of a music track.
  */
sealed trait Change extends HasRelativePath {

  /**
    * The relative path of the file that changed.
    */
  val relativePath: RelativePath

  /**
    * The time the change occurred.
    */
  val at: Instant
}

/**
  * A change that represents a music track needs to be added.
  * @param relativePath The relative path of the music track.
  * @param at The time the track was added.
  * @param links [[Links]] that can be used to get information about the new track.
  */
case class Addition(relativePath: RelativePath, at: Instant, links: Links) extends Change with HasLinks

/**
  * A change that represents a music track needs to be removed.
  * @param relativePath The relative path of the music track.
  * @param at The time the track was removed.
  */
case class Removal(relativePath: RelativePath, at: Instant) extends Change
