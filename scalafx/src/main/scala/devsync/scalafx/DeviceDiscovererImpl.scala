package devsync.scalafx
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{Files, Path}
import java.util.function.BiPredicate

import devsync.json.DeviceDescriptor
import devsync.sync.Device
import scala.collection.JavaConversions._
import devsync.scalafx.PathResource._

/**
  * Created by alex on 09/04/17
  * The default device discoverer that looks for a device descriptor in a directory, searching at most n levels down
  **/
class DeviceDiscovererImpl(val device: Device[Path]) extends DeviceDiscoverer {

  override def discover(root: Path, levels: Int): Either[Exception, (DeviceDescriptor, Path)] = {
    val directoryPredicate = new BiPredicate[Path, BasicFileAttributes] {
      override def test(p: Path, bfa: BasicFileAttributes): Boolean = bfa.isDirectory
    }
    val possibleRoots = Files.find(root, levels, directoryPredicate).iterator().toSeq
    device.findDeviceDescriptor(possibleRoots)
  }
}