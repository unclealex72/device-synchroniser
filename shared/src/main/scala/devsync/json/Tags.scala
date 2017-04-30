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
  * A case class that holds information about a music track.
  * @param albumArtistSort The album artist used for sorting.
  * @param albumArtist The album artist of the track.
  * @param album The album the track is on.
  * @param artist The artist who play the track.
  * @param artistSort The artist sort used for sorting.
  * @param title The name of the track.
  * @param totalDiscs The total number of discs that comprise the album this track is on.
  * @param totalTracks The total number of tracks on the disc.
  * @param discNumber The disc number for this track.
  * @param albumArtistId The [[http://www.musicbrainz.org MusicBrainz]] ID for the album's artist.
  * @param albumId The [[http://www.musicbrainz.org MusicBrainz]] ID for the album.
  * @param artistId The [[http://www.musicbrainz.org MusicBrainz]] ID for the track's artist.
  * @param trackId The [[http://www.musicbrainz.org MusicBrainz]] ID for the track.
  * @param asin The [[http://www.amazon.co.uk Amazon]] _ASIN_ for this album, if any.
  * @param trackNumber The number of this track on the album.
  */
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

  /**
    * Format an album so that it contains the disc number and total number of discs for multi-disc albums.
    * @return The title of the album formatted as above.
    */
  def formattedAlbum: String = {
    if (totalDiscs == 1) {
      album
    }
    else {
      s"$album ($discNumber of $totalDiscs)"
    }
  }

  /**
    * Format a track so that it contains the track number, total number of tracks and track title.
    * @return A formatted track as described above.
    */
  def formattedTrack: String = s"$trackNumber/$totalTracks - $title"
}