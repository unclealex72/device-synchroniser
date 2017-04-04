package uk.co.unclealex.devsync

import android.app.Activity
import android.content.{Context, Intent}
import android.net.Uri
import android.os.Bundle
import android.support.v4.provider.DocumentFile
import android.view.ViewGroup.LayoutParams._
import android.widget._
import devsync.common.PassthroughLogging
import devsync.sync.DeviceImpl
import macroid.FullDsl._
import macroid._
import uk.co.unclealex.devsync.Async._

import scala.util.{Failure, Success}
import cats.syntax.either._

import IntentHelper._
import DocumentFileResource._

class DeviceDiscoveryActivity extends Activity with Contexts[Activity] with PassthroughLogging {

  var findDeviceButton: Option[Button] = slot[Button]
  var progressBar: Option[ProgressBar] = slot[ProgressBar]
  var errorMessage: Option[TextView] = slot[TextView]

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)

    // the layout goes here
    setContentView {
      //
      Ui.get {
        l[LinearLayout](
          w[ProgressBar] <~ wire(progressBar) <~ lp[LinearLayout](MATCH_PARENT, WRAP_CONTENT) <~ hide,
          w[Button] <~ wire(findDeviceButton) <~ text("Find Device") <~ hide <~ On.click {
            Ui {
              val intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
              intent.setFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
              startActivityForResult(intent, 0)
            }
          },
          w[TextView] <~ wire(errorMessage) <~ text("err!") <~ hide
        ) <~ vertical
      }
    }
    val prefs = getPreferences(Context.MODE_PRIVATE)
    val maybeDeviceUri = Option(prefs.getString(Constants.Prefs.resourceUri, null))
    processDeviceDescriptor(maybeDeviceUri)
  }

  private def processDeviceDescriptor(maybeDeviceUri: Option[String]): Unit = {
    implicit val resourceStreamProvider: DocumentFileResourceStreamProvider = new DocumentFileResourceStreamProvider()
    val device = Services.device
    val maybeDeviceResourceAndUri: Option[(DocumentFile, String)] = for {
      uri <- maybeDeviceUri
      deviceResource <- Some(DocumentFile.fromTreeUri(this, Uri.parse(uri)))
    } yield (deviceResource, uri)
      val tryDeviceDescriptorAndUri =
        device.findDeviceDescriptor(maybeDeviceResourceAndUri.map(_._1)).map { deviceDescriptor =>
          DeviceDescriptorAndUri(deviceDescriptor, maybeDeviceResourceAndUri.map(_._2).getOrElse(""))
        }
      tryDeviceDescriptorAndUri match {
        case Left(e) =>
          Ui.run((findDeviceButton <~ show) ~ (errorMessage <~ text(e.getMessage) <~ show))
        case Right(deviceDescriptorAndUri) =>
          Ui.run(progressBar <~ show).flatMap { _ => Services.flacManagerDiscovery.discover }.onCompleteUi {
            case Success(url) => Ui(next(deviceDescriptorAndUri, url.toString))
            case Failure(e) => errorMessage <~ text(e.getMessage) <~ show
          }
      }
  }

  override def onActivityResult(requestCode: Int, resultCode: Int, data: Intent): Unit = {
    val treeUri = data.getData
    getContentResolver.takePersistableUriPermission(
      treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    val prefs = getPreferences(Context.MODE_PRIVATE)
    prefs.edit().putString(Constants.Prefs.resourceUri, treeUri.toString).commit()
    processDeviceDescriptor(Some(treeUri.toString))
  }

  def next(deviceDescriptorAndUri: DeviceDescriptorAndUri, serverUrl: String): Unit = {
    val intent = new Intent(this, classOf[DeviceSynchroniserActivity])
    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK)
    intent.put(deviceDescriptorAndUri.deviceDescriptor, deviceDescriptorAndUri.uri, serverUrl)
    startActivity(intent)
  }

}