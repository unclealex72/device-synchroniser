package uk.co.unclealex.devsync

import android.app.Activity
import android.content.{Context, Intent}
import android.net.Uri
import android.os.Bundle
import android.support.v4.provider.DocumentFile
import android.view.ViewGroup.LayoutParams._
import android.widget._
import devsync.json.DeviceDescriptor
import devsync.sync.{Device, Resource}
import macroid.FullDsl._
import macroid._

import scala.util.{Failure, Success}

class DeviceSynchroniser extends Activity with Contexts[Activity] {

  var findDeviceButton: Option[Button] = slot[Button]
  var progressBar: Option[ProgressBar] = slot[ProgressBar]
  var errorMessage: Option[TextView] = slot[TextView]

  var deviceDescriptor: Option[DeviceDescriptor] = None

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
    val maybeDeviceUri = Option(prefs.getString("deviceUri", null))
    processDeviceDescriptor(maybeDeviceUri)

    /*
    new ClingFlacManagerDiscovery(new AndroidUpnpServiceConfiguration).discover.onSuccess { case url =>
      runOnUiThread(meToo.text = url.toString)b
    }
    val prefs = getPreferences(Context.MODE_PRIVATE)
    val deviceUri = prefs.getString("deviceUri", "undefined")
    editText.text = deviceUri
    */
  }

  private def processDeviceDescriptor(maybeDeviceUri: Option[String]): Unit = {
    val maybeDeviceResource: Option[Resource] =
      maybeDeviceUri.
        map(uri => DocumentFile.fromTreeUri(this, Uri.parse(uri))).
        map(documentFile => new DocumentFileResource(documentFile))
      val tryDeviceDescriptor = Device.findDeviceDescriptor(Services.jsonCodec, maybeDeviceResource)
      val action = tryDeviceDescriptor match {
        case Failure(e) =>
          (findDeviceButton <~ show) ~ (errorMessage <~ text(e.getMessage) <~ show)
        case Success(deviceDescriptor) =>
          progressBar <~ show
      }
    Ui.run(action)
  }

  override def onActivityResult(requestCode: Int, resultCode: Int, data: Intent): Unit = {
    val treeUri = data.getData
    getContentResolver.takePersistableUriPermission(
      treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    val prefs = getPreferences(Context.MODE_PRIVATE)
    prefs.edit().putString("deviceUri", treeUri.toString).commit()
    processDeviceDescriptor(Some(treeUri.toString))
  }
}