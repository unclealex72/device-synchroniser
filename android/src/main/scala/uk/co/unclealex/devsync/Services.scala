package uk.co.unclealex.devsync

import java.net.URL

import android.support.v4.provider.DocumentFile
import devsync.discovery.{ClingFlacManagerDiscovery, FlacManagerDiscovery}
import devsync.json.{CirceCodec, IsoClock, JsonCodec, SystemIsoClock}
import devsync.remote.{ChangesClient, ChangesClientImpl}
import devsync.sync.{Device, DeviceImpl}
import org.fourthline.cling.android.AndroidUpnpServiceConfiguration

/**
  * Created by alex on 26/03/17
  * An object that holds singleton services
  **/
object Services {

  val jsonCodec: JsonCodec = new CirceCodec

  val flacManagerDiscovery: FlacManagerDiscovery = new ClingFlacManagerDiscovery(new AndroidUpnpServiceConfiguration)

  def changesClient(baseUrl: URL): ChangesClient = new ChangesClientImpl(jsonCodec, baseUrl)

  val isoClock: IsoClock = SystemIsoClock

  val device: Device[DocumentFile] = new DeviceImpl[DocumentFile](jsonCodec, isoClock)

}
