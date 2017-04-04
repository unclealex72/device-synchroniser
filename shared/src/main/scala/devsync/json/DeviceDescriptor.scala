package devsync.json

/**
  * Created by alex on 24/03/17
  * A class that defines the JSON that is stored on a device to find out if it needs to be serialised.
  **/
case class DeviceDescriptor(user: String, maybeLastModified: Option[IsoDate], maybeOffset: Option[Int]) extends Serializable {

  def withLastModified(lastModified: IsoDate): DeviceDescriptor = this.copy(maybeLastModified = Some(lastModified))

  def withOffset(offset: Int): DeviceDescriptor = this.copy(maybeOffset = Some(offset))
}
