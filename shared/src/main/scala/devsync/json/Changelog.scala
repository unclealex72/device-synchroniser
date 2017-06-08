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
  * A changelog is a list of changes teCreated by alex on 19/03/17hat will be applied to a device at an album level.
 *
  * @param items The list of changes.
  **/
case class Changelog(items: Seq[ChangelogItem])

/**
  * A changelog item represents a potential change to an album on a device.
  * @param parentRelativePath The parent relative path of the change.
  * @param at The time the change was made.
  * @param relativePath The relative path of the album that has been added or removed.
  * @param links A set of [[Links]] to read information about the album.
  */
case class ChangelogItem(parentRelativePath: RelativePath,
                         at: Instant,
                         relativePath: RelativePath, links: Links) extends HasLinks with HasRelativePath
