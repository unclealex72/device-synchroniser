package devsync.discovery
import java.net.URL

import cats.data.EitherT
import com.typesafe.scalalogging.StrictLogging
import org.fourthline.cling.model.message.header.UDADeviceTypeHeader
import org.fourthline.cling.model.meta.RemoteDevice
import org.fourthline.cling.model.types.{UDADeviceType, UDAServiceId}
import org.fourthline.cling.registry.{DefaultRegistryListener, Registry}
import org.fourthline.cling.{UpnpServiceConfiguration, UpnpServiceImpl}

import scala.concurrent.{ExecutionContext, Future, Promise}

/**
  * An instance of [[FlacManagerDiscovery]] that uses [[https://github.com/4thline/cling Cling]]
  **/
class ClingFlacManagerDiscovery(upnpServiceConfiguration: UpnpServiceConfiguration) extends FlacManagerDiscovery with StrictLogging {

  /**
    * @inheritdoc
    */
  override def discover(dev: Boolean)(implicit ec: ExecutionContext): EitherT[Future, Exception, URL] = EitherT {

    val urlPromise: Promise[URL]  = Promise()
    val upnpService = new UpnpServiceImpl(upnpServiceConfiguration)
    val identifier = "FlacManagerService" + (if (dev) "Dev" else "")
    val udaType = new UDADeviceType(identifier)
    val serviceId = new UDAServiceId(identifier)
    upnpService.getRegistry.addListener(new DefaultRegistryListener {
      override def remoteDeviceAdded(registry: Registry, device: RemoteDevice): Unit = execute(device)
      override def remoteDeviceUpdated(registry: Registry, device: RemoteDevice): Unit = execute(device)
      def execute(device: RemoteDevice): Unit = {
        Option(device.findService(serviceId)).foreach { service =>
          def executeAction[T](argumentName: String, promise: Promise[T], responseTransformer: String => T): Unit = {
            ServerDetailsHelper.executeGetter(upnpService.getControlPoint, service, argumentName, promise, responseTransformer)
          }
          executeAction("Url", urlPromise, new URL(_))
        }
      }
    })
    upnpService.getControlPoint.search(new UDADeviceTypeHeader(udaType))
    urlPromise.future.map { url =>
      Future { upnpService.shutdown() }
      Right(url)
    }.recover {
      case e: Exception => Left(e)
    }
  }
}
