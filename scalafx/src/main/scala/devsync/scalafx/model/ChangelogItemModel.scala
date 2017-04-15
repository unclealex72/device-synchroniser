package devsync.scalafx.model

import devsync.json.{IsoDate, RelativePath}

/**
  * Created by alex on 15/04/17
  **/
case class ChangelogItemModel(at: IsoDate, maybeArtwork: Option[Array[Byte]], relativePathOrAlbum: Either[RelativePath, Album])
case class Album(album: String, artist: String)
