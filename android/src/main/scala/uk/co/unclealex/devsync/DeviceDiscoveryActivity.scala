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
import android.widget.ViewSwitcher
import cats.syntax.either._
import devsync.logging.PassthroughLogging
import macroid._
import uk.co.unclealex.devsync.Async._
import uk.co.unclealex.devsync.DocumentFileResource._
import uk.co.unclealex.devsync.IntentHelper._

import scala.concurrent.duration._
import scala.util.Success
import android.Manifest.permission
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat

/**
  * An activity that shows a progress spinner whilst searching for a Flac Manager server and
  * device descriptor file.
  */
class DeviceDiscoveryActivity extends Activity with Contexts[Activity] with PassthroughLogging {

  private val PERMISSIONS_REQUEST: Int = 0

  /**
    * @inheritdoc
    */
  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.device_discovery_activity)
    checkPermissions()
  }

  override def onRequestPermissionsResult(requestCode: Int, permissions: Array[String], grantResults: Array[Int]): Unit = {
    checkPermissions()
  }

  def checkPermissions(): Unit = {
    val requiredPermissions = Seq(
      permission.READ_EXTERNAL_STORAGE,
      permission.WRITE_EXTERNAL_STORAGE,
      permission.INTERNET,
      permission.ACCESS_WIFI_STATE,
      permission.CHANGE_WIFI_MULTICAST_STATE,
      permission.ACCESS_NETWORK_STATE,
      permission.WAKE_LOCK)
    val missingPermissions = requiredPermissions.filter { pm =>
      ContextCompat.checkSelfPermission(this, pm) != PackageManager.PERMISSION_GRANTED
    }
    if (missingPermissions.isEmpty) {
      val prefs = getPreferences(Context.MODE_PRIVATE)
      val maybeDeviceUri = Option(prefs.getString(Constants.Prefs.resourceUri, null))
      processDeviceDescriptor(maybeDeviceUri)
    }
    else {
      ActivityCompat.requestPermissions(this, missingPermissions.toArray[String], PERMISSIONS_REQUEST)
    }
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
        DialogBuilder.
          title(R.string.no_device_descriptor_title).
          message(Seq(e.getMessage, "Please search").mkString("\n")).
          positiveButton(R.string.search) {
            val intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            intent.setFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            startActivityForResult(intent, 0)
          }.
          show
      case Right(deviceDescriptorAndUri) =>
        val dev = Option(getIntent.getExtras).exists(_.getBoolean("FLAC_DEV"))
        Services.flacManagerDiscovery.discover(dev, 30.seconds).value.onComplete {
          case Success(Right(url)) =>
            Ui(next(deviceDescriptorAndUri, url.toString)).run
          case _ =>
            DialogBuilder.
              title(R.string.no_flac_manager_service_title).
              message(R.string.no_flac_manager_service_message).
              positiveButton(R.string.yes) {
                processDeviceDescriptor(Some(deviceDescriptorAndUri.uri))
              }.
              negativeButton(R.string.no) {
                this.finishAndRemoveTask()
              }.show
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
    val intent = new Intent(this, classOf[DeviceSynchroniserActivity]).
      setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK).
      putResourceUri(deviceDescriptorAndUri.uri).
      putServerUrl(serverUrl)
    startActivity(intent)
  }
}