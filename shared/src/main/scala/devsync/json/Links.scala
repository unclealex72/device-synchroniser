package devsync.json

import java.net.URL

/**
  * Created by alex on 21/03/17
  **/
case class Links(
                  /**
                    * A link to the music data.
                    */
                  music: URL,
                  /**
                    * A link to the tags for the music file.
                    */
                  tags: URL,
                  /**
                    * A link to the artwork for the track.
                    */
                  artwork: URL) extends HasLinks {
  override val links: Links = this
}

/**
  * A trait for classes that have the links as above.
  */
trait HasLinks {
  val links: Links
}