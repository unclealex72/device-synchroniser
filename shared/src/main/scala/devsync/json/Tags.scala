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
  * Created by alex on 21/03/17
  **/
case class Tags(
  albumArtistSort: String,
  albumArtist: String,
  album: String,
  artist: String,
  artistSort: String,
  title: String,
  totalDiscs: Int,
  totalTracks: Int,
  discNumber: Int,
  albumArtistId: String,
  albumId: String,
  artistId: String,
  trackId: String,
  asin: Option[String],
  trackNumber: Int) {

  def formattedAlbum: String = {
    if (totalDiscs == 1) {
      album
    }
    else {
      s"$album ($discNumber of $totalDiscs)"
    }
  }

  def formattedTrack: String = s"$trackNumber/$totalTracks - $title"
}