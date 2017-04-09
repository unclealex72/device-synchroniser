package devsync.json

/**
  * Created by alex on 20/03/17
  *
  * An trait that can read and parse changelogs changes.
  **/
trait JsonCodec {

  def parseChangelog(json: String): Either[Exception, Changelog]

  def parseChanges(json: String): Either[Exception, Changes]
  
  def parseTags(json: String): Either[Exception, Tags]

  def parseDeviceDescriptor(json: String): Either[Exception, DeviceDescriptor]

  def writeDeviceDescriptor(deviceDescriptor: DeviceDescriptor): String
}
