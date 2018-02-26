/*
 * Copyright 2017 Alex Jones
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package devsync.discovery
import java.net.URL
import java.util.concurrent.TimeoutException

import cats.data.EitherT
import com.typesafe.scalalogging.StrictLogging
import devsync.monads.FutureEither
import org.fourthline.cling.model.message.header.UDADeviceTypeHeader
import org.fourthline.cling.model.meta.RemoteDevice
import org.fourthline.cling.model.types.{UDADeviceType, UDAServiceId}
import org.fourthline.cling.registry.{DefaultRegistryListener, Registry}
import org.fourthline.cling.{UpnpServiceConfiguration, UpnpServiceImpl}

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Try

/**
  * An instance of [[FlacManagerDiscovery]] that uses [[https://github.com/4thline/cling Cling]]
  * @param upnpServiceConfiguration The [[UpnpServiceConfiguration]] to use. This is different for Android and desktop.
  **/
class ClingFlacManagerDiscovery(upnpServiceConfiguration: UpnpServiceConfiguration) extends FlacManagerDiscovery with StrictLogging {

  /**
    * @inheritdoc
    */
  override def discover(dev: Boolean, timeout: Duration)(implicit ec: ExecutionContext): FutureEither[Exception, URL] = EitherT {

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
    val timeoutPromise: Promise[URL] = Promise()
    val timeoutThread = new Thread {
      override def run(): Unit = {
        try {
          Thread.sleep(timeout.toMillis)
          timeoutPromise.failure(new TimeoutException("Could not find a Flac Manager server within " + timeout))
        }
        catch {
          case _: InterruptedException => timeoutPromise.success(new URL("http://localhost"))
        }
      }
    }
    timeoutThread.start()
    val timeoutFuture: Future[URL] = timeoutPromise.future
    val urlFuture: Future[URL] = urlPromise.future
    val timingOutUrlFuture = Future.firstCompletedOf(Seq(timeoutFuture, urlFuture)).map(Right(_))
    timingOutUrlFuture.map { value =>
      Future {
        Try(upnpService.shutdown())
        Try(timeoutThread.interrupt())
      }
      value
    }.recover {
      case e: Exception => Left(e)
    }
  }
}
