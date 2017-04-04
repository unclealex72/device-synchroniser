package uk.co.unclealex.devsync

import android.content.Intent
import devsync.json.DeviceDescriptor

/**
  * Created by alex on 02/04/17
  **/
object IntentHelper {

  implicit class IntentHelperImplicits(intent: Intent) {

    def put(deviceDescriptor: DeviceDescriptor, resourceUri: String, serverUrl: String): Unit = {
      intent.putExtra(Constants.Intent.deviceDescriptor, deviceDescriptor)
      intent.putExtra(Constants.Intent.resourceUri, resourceUri)
      intent.putExtra(Constants.Intent.serverUrl, serverUrl)
    }

    def deviceDescriptor: DeviceDescriptor = {
      intent.getExtras.getSerializable(Constants.Intent.deviceDescriptor).asInstanceOf[DeviceDescriptor]
    }

    def resourceUri: String = {
      intent.getExtras.getString(Constants.Intent.resourceUri)
    }

    def serverUrl: String = {
      intent.getExtras.getString(Constants.Intent.serverUrl)
    }
  }
}
