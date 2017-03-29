package devsync.json

import scala.util.Try

/**
  * Created by alex on 20/03/17
  *
  * An trait that can read and parse changelogs changes.
  **/
trait JsonCodec {

  def parseChangelog(json: String): Try[Changelog]

  def parseChangelogCount(json: String): Try[ChangelogCount]

  def parseChanges(json: String): Try[Changes]
  
  def parseTags(json: String): Try[Tags]

  def parseDeviceDescriptor(json: String): Try[DeviceDescriptor]

  def writeDeviceDescriptor(deviceDescriptor: DeviceDescriptor): String
}
