package devsync.scalafx

import java.nio.file.Path

import devsync.json.DeviceDescriptor

/**
  * Created by alex on 09/04/17
  *
  * A trait used to find devices somewhere arbitrarily on file system
  **/
trait DeviceDiscoverer {

  def discover(root: Path, levels: Int): Either[Exception, (DeviceDescriptor, Path)]
}
