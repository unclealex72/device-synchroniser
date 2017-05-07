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

package devsync.scalafx

import devsync.json.{IsoDate, RelativePath, Tags}

import scalafx.beans.property.ObjectProperty

/**
  * A model for a [[devsync.json.ChangelogItem]]
  *
  * @param at The time of the change.
  * @param maybeArtwork The album artwork if it could be loaded.
  * @param maybeTags The tags for a track on the album if they could be loaded.
  * @param albumRelativePath The relative path of the album.
  **/
case class ChangelogItemModel(
                               at: IsoDate,
                               maybeArtwork: Option[Array[Byte]],
                               maybeTags: Option[Tags],
                               albumRelativePath: RelativePath) {

  val progress: ObjectProperty[Option[(Long, Long)]] = new ObjectProperty[Option[(Long, Long)]](None, "")
}