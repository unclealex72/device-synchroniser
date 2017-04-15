package devsync.scalafx.model

import devsync.json.RelativePath

/**
  * Created by alex on 14/04/17
  **/
sealed trait SynchroniseAction

case class SynchroniseAddition(
                                maybeArtwork: Option[Array[Byte]],
                                artist: String,
                                album: String,
                                track: String) extends SynchroniseAction

case class SynchroniseRemoval(relativePath: RelativePath) extends SynchroniseAction

