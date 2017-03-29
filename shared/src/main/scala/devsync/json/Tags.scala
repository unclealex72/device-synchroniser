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
  trackNumber: Int)