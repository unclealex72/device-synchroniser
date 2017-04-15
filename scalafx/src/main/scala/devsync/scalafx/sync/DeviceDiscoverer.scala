package devsync.scalafx.sync

import java.nio.file.Path

import cats.data.EitherT
import devsync.json.DeviceDescriptor

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by alex on 09/04/17
  *
  * A trait used to find devices somewhere arbitrarily on file system
  **/
trait DeviceDiscoverer {

  def discover(root: Path, levels: Int)(implicit ec: ExecutionContext): EitherT[Future, Exception, (DeviceDescriptor, Path)]
}
