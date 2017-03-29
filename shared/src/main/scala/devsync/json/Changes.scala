package devsync.json

/**
  * Created by alex on 19/03/17
  **/
case class Changes(changes: Seq[Change])

sealed trait Change {
  /**
    * The relative path of the file that changed.
    */
  val relativePath: RelativePath
  /**
    * The time the change occurred.
    */
  val at: IsoDate
}

case class Addition(relativePath: RelativePath, at: IsoDate, links: Links) extends Change


case class Removal(relativePath: RelativePath, at: IsoDate) extends Change
