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

package uk.co.unclealex.devsync

import android.app.Activity
import android.content.{Context, Intent}
import android.net.Uri
import android.os.Bundle
import android.support.v4.provider.DocumentFile
import android.view.ViewGroup.LayoutParams._
import android.widget._
import devsync.common.{Messages, PassthroughLogging}
import devsync.sync.DeviceImpl
import macroid.FullDsl._
import macroid._
import uk.co.unclealex.devsync.Async._

import scala.util.{Failure, Success}
import cats.syntax.either._
import IntentHelper._
import DocumentFileResource._

/**
  * An activity that shows a progress spinner whilst searching for a Flac Manager server and
  * device descriptor file.
  */
class DeviceDiscoveryActivity extends Activity with Contexts[Activity] with PassthroughLogging {

  var findDeviceButton: Option[Button] = slot[Button]
  var progressBar: Option[ProgressBar] = slot[ProgressBar]
  var errorMessage: Option[TextView] = slot[TextView]

  /**
    * @inheritdoc
    */
  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)

    // the layout goes here
    setContentView {
      //
      Ui.get {
        l[LinearLayout](
          w[ProgressBar] <~ wire(progressBar) <~ lp[LinearLayout](MATCH_PARENT, WRAP_CONTENT) <~ hide,
          w[Button] <~ wire(findDeviceButton) <~ text(Messages.Discovery.findDevice) <~ hide <~ On.click {
            Ui {
              val intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
              intent.setFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
              startActivityForResult(intent, 0)
            }
          },
          w[TextView] <~ wire(errorMessage) <~ hide
        ) <~ vertical
      }
    }
    val prefs = getPreferences(Context.MODE_PRIVATE)
    val maybeDeviceUri = Option(prefs.getString(Constants.Prefs.resourceUri, null))
    processDeviceDescriptor(maybeDeviceUri)
  }

  /**
    * Look for a device descriptor file and, if found, look for the Flac Manager server.
    * @param maybeDeviceUri The device URI from preferences or none if the preferences did not contain a device URI.
    */
  private def processDeviceDescriptor(maybeDeviceUri: Option[String]): Unit = {
    implicit val resourceStreamProvider: DocumentFileResourceStreamProvider = new DocumentFileResourceStreamProvider()
    val device = Services.device
    val maybeDeviceResource: Option[DocumentFile] = for {
      uri <- maybeDeviceUri
      deviceResource <- Some(DocumentFile.fromTreeUri(this, Uri.parse(uri)))
    } yield deviceResource
    val tryDeviceDescriptorAndUri =
      device.findDeviceDescriptor(maybeDeviceResource).map {
        case (deviceDescriptor, documentRoot) =>
          DeviceDescriptorAndUri(deviceDescriptor, documentRoot.getUri.toString)
      }
    tryDeviceDescriptorAndUri match {
      case Left(e) =>
        Ui.run((findDeviceButton <~ show) ~ (errorMessage <~ text(e.getMessage) <~ show))
      case Right(deviceDescriptorAndUri) =>
        val dev = getIntent.getExtras.getBoolean("FLAC_DEV")
        Ui.run(progressBar <~ show).flatMap { _ => Services.flacManagerDiscovery.discover(dev).value }.onCompleteUi {
          case Success(Right(url)) =>
            Ui(next(deviceDescriptorAndUri, url.toString))
          case Success(Left(e)) =>
            errorMessage <~ text(e.getMessage) <~ show
          case Failure(e) =>
            errorMessage <~ text(e.getMessage) <~ show
        }
    }
  }

  /**
    * Store the value of the location of the device descriptor file.
    * @param requestCode N/A
    * @param resultCode N/A
    * @param data The URI of the location of the device descriptor file.
    */
  override def onActivityResult(requestCode: Int, resultCode: Int, data: Intent): Unit = {
    val treeUri = data.getData
    getContentResolver.takePersistableUriPermission(
      treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    val prefs = getPreferences(Context.MODE_PRIVATE)
    prefs.edit().putString(Constants.Prefs.resourceUri, treeUri.toString).commit()
    processDeviceDescriptor(Some(treeUri.toString))
  }

  /**
    * Move on to the [[DeviceSynchroniserActivity]]
    * @param deviceDescriptorAndUri The device descriptor and its location.
    * @param serverUrl The URL of the Flac Manager server.
    */
  def next(deviceDescriptorAndUri: DeviceDescriptorAndUri, serverUrl: String): Unit = {
    val intent = new Intent(this, classOf[DeviceSynchroniserActivity])
    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK)
    intent.put(deviceDescriptorAndUri.deviceDescriptor, deviceDescriptorAndUri.uri, serverUrl)
    startActivity(intent)
  }

}