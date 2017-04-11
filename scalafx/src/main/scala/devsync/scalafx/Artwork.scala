package devsync.scalafx

import java.io.ByteArrayInputStream

import scalafx.scene.image.Image

/**
  * Generate artwork from a source.
  * Created by alex on 11/04/17
  **/
object Artwork {

  def apply(maybeData: Option[Array[Byte]]): Image = {
    maybeData match {
      case Some(data) => new Image(new ByteArrayInputStream(data))
      case _ => new Image("/no_artwork.png")
    }
  }
}
