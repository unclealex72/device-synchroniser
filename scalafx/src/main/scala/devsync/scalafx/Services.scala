package devsync.scalafx

import java.net.URL
import java.nio.file.Path

import devsync.discovery.{ClingFlacManagerDiscovery, FlacManagerDiscovery}
import devsync.json._
import devsync.remote.{ChangesClient, ChangesClientImpl}
import devsync.sync._
import org.fourthline.cling.DefaultUpnpServiceConfiguration

/**
  * Created by alex on 09/04/17
  **/
object Services {

  val jsonCodec: JsonCodec = new CirceCodec
  val isoClock: IsoClock = SystemIsoClock
  val flacManagerDiscovery: FlacManagerDiscovery = new ClingFlacManagerDiscovery(new DefaultUpnpServiceConfiguration())
  val device: Device[Path] = new DeviceImpl[Path](jsonCodec, isoClock)
  def changesClient(url: URL): ChangesClient = new ChangesClientImpl(jsonCodec, url)

}
