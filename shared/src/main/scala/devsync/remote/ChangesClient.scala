package devsync.remote

import java.io.OutputStream

import devsync.json._

import scala.util.Try

/**
  * Created by alex on 22/03/17
  * A trait that can be used to find changes and changelogs
  **/
trait ChangesClient {

  /**
    * Get the changes for a user since a specific date.
    */
  def changesSince(user: String, since: IsoDate): Try[Changes]

  /**
    * Count the number of changelog items for a user since a specific date
    */
  def countChangelogSince(user: String, since: IsoDate): Try[Int]

  /**
    * Get a page of a changelog for a user.
    */
  def changelog(user: String, page: Int, limit: Int): Try[Changelog]

  def music(addition: Addition, out: OutputStream): Try[Unit]

  def tags(addition: Addition): Try[Tags]

  def artwork(addition: Addition, out: OutputStream): Try[Unit]
}
