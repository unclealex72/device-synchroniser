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

package devsync.scalafx.model

import devsync.json.{IsoDate, RelativePath}

/**
  * A model for a [[devsync.json.ChangelogItem]]
  * @param at The time of the change.
  * @param maybeArtwork The album artwork if it could be loaded.
  * @param relativePathOrAlbum Either the relative path of the change or the album.
  **/
case class ChangelogItemModel(at: IsoDate, maybeArtwork: Option[Array[Byte]], relativePathOrAlbum: Either[RelativePath, Album])

/**
  * A model for an album.
  * @param album The name of the album.
  * @param artist The album's artist.
  */
case class Album(album: String, artist: String)
