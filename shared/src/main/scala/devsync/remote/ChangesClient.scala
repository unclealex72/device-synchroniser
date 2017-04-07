package devsync.remote

import java.io.OutputStream

import devsync.json._

/**
  * Created by alex on 22/03/17
  * A trait that can be used to find changes and changelogs
  **/
trait ChangesClient {

  /**
    * Get the changes for a user since a specific date.
    */
  def changesSince(user: String, maybeSince: Option[IsoDate]): Either[Exception, Changes]

  /**
    * Count the number of changelog items for a user since a specific date
    */
  def changelogSince(user: String, maybeSince: Option[IsoDate]): Either[Exception, Changelog]

  def music(item: HasLinks with HasRelativePath, out: OutputStream): Either[Exception, Unit]

  def tags(item: HasLinks with HasRelativePath): Either[Exception, Tags]

  def artwork(item: HasLinks with HasRelativePath, out: OutputStream): Either[Exception, Unit]
}
