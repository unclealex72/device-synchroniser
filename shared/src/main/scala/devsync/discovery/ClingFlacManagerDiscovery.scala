package devsync.discovery
import java.net.URL

import cats.data.EitherT
import com.typesafe.scalalogging.StrictLogging
import org.fourthline.cling.{UpnpService, UpnpServiceConfiguration, UpnpServiceImpl}
import org.fourthline.cling.model.message.header.UDADeviceTypeHeader
import org.fourthline.cling.model.meta.RemoteDevice
import org.fourthline.cling.model.types.UDADeviceType
import org.fourthline.cling.registry.{DefaultRegistryListener, Registry}

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Try

/**
  * Created by alex on 23/03/17
  **/
class ClingFlacManagerDiscovery(upnpServiceConfiguration: UpnpServiceConfiguration) extends FlacManagerDiscovery with StrictLogging {

  /**
    * Try and find a flac manager on the local network.
    *
    * @return A future eventually containing the root url or an error.
    */
  override def discover(implicit ec: ExecutionContext): EitherT[Future, Exception, URL] = {

    import cats.instances.future.catsStdInstancesForFuture

    val eventualAnswer: Promise[URL] = Promise()

    val udaType = new UDADeviceType("FlacManager")
    val listener = new DefaultRegistryListener {
      override def remoteDeviceAdded(registry: Registry, device: RemoteDevice): Unit = {
        if (device.getType == udaType) {
          val url = device.getDetails.getPresentationURI.toURL
          logger.info(s"Found Flac Manager with presentation URL $url")
          eventualAnswer.complete(Try(url))
        }
      }
    }
    val upnpService: UpnpService = new UpnpServiceImpl(upnpServiceConfiguration, listener)
    upnpService.getControlPoint.search(new UDADeviceTypeHeader(udaType))
    EitherT.right {
      eventualAnswer.future.map { url =>
        Future {
          upnpService.shutdown()
        }
        url
      }
    }
  }
}
