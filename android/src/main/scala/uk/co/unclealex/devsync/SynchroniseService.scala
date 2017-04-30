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

import java.net.URL

import android.app._
import android.content.{Context, Intent}
import android.net.Uri
import android.support.v4.provider.DocumentFile
import com.typesafe.scalalogging.StrictLogging
import devsync.logging.Messages
import devsync.json._
import devsync.sync.DeviceListener
import macroid.Contexts
import uk.co.unclealex.devsync.Async._
import uk.co.unclealex.devsync.DocumentFileResource._
import uk.co.unclealex.devsync.IntentHelper._

/**
  * Download music from a Flac Manager server and show progress in a notification.
  **/
class SynchroniseService extends IntentService("devsync") with Contexts[Service] with StrictLogging {

  /**
    * The Android [[NotificationManager]]
    */
  lazy val notificationManager: NotificationManager =
    getSystemService(Context.NOTIFICATION_SERVICE).asInstanceOf[NotificationManager]

  /**
    * The width of the album art icon.
    */
  lazy val iconWidth: Int = getResources.getDimensionPixelSize(android.R.dimen.notification_large_icon_width)

  /**
    * The height of the album art icon.
    */
  lazy val iconHeight: Int = getResources.getDimensionPixelSize(android.R.dimen.notification_large_icon_height)

  /**
    * The ID of the generated notification.
    */
  val ONGOING_NOTIFICATION_ID = 1

  /**
    * Start synchronising.
    * @param intent An intent containing the location of the Flac Manager and device directory.
    */
  override def onHandleIntent(intent: Intent): Unit = {
    val serverUrl = new URL(intent.serverUrl)
    val resourceUri = intent.resourceUri
    val rootDocumentFile = DocumentFile.fromTreeUri(this, Uri.parse(resourceUri))
    val changesClient = Services.changesClient(serverUrl)
    val deviceListener = createNewDeviceListener
    val device = Services.device
    implicit val resourceStreamProvider: DocumentFileResourceStreamProvider = new DocumentFileResourceStreamProvider()
    device.synchronise(rootDocumentFile, changesClient, deviceListener)
  }

  /**
    * A case class used to send information to a notification when a new track is added or removed.
    * @param change The [[Change]] action.
    * @param maybeTags The music file's tags, if any.
    * @param maybeArtwork The music file's artwork, if any.
    * @param number The index of this change.
    * @param total The total number of changes that will occur.
    */
  case class ChangeModel(
                          change: Change,
                          maybeTags: Option[Tags],
                          maybeArtwork: Option[Array[Byte]],
                          number: Int,
                          total: Int)
  /**
    * A [[DeviceListener]] that displays progress on a notification.
    * @return A new [[DeviceListener]]
    */
  def createNewDeviceListener: DeviceListener[DocumentFile] = new DeviceListener[DocumentFile] {

    /**
      * Create a new notification.
      */
    override def synchronisingStarting(): Unit = {
      val notification = buildNotification(Left("Starting"))
      startForeground(ONGOING_NOTIFICATION_ID, notification)
      notificationManager.notify(ONGOING_NOTIFICATION_ID, notification)
    }

    /**
      * Notify that a track is about to be removed.
      * @param removal The [[Removal]] information.
      * @param number The index of this change.
      * @param total The total number of changes.
      */
    override def removingMusic(removal: Removal, number: Int, total: Int): Unit = {
      buildAndNotify(Right(ChangeModel(removal, None, None, number, total)))
    }

    override def musicRemoved(removal: Removal, number: Int, total: Int): Unit = {}

    /**
      * Notify that a track is about to be added.
      * @param addition The [[Addition]] information.
      * @param maybeTags The tags for the track about to be added, if any.
      * @param maybeArtwork The artwork for the track about to be added, if any.
      * @param number The index of this change.
      * @param total The total number of changes.
      */
    override def addingMusic(addition: Addition,
                             maybeTags: Option[Tags],
                             maybeArtwork: Option[Array[Byte]], number: Int, total: Int): Unit = {
      buildAndNotify(Right(ChangeModel(addition, maybeTags, maybeArtwork, number, total)))
    }

    /**
      * Broadcast an event to the Media Scanner that a new music file has been added.
      * @param addition The [[Addition]] information.
      * @param maybeTags The tags for the track about to be added, if any.
      * @param maybeArtwork The artwork for the track about to be added, if any.
      * @param number The index of this change.
      * @param resource The newly added resource.
      */
    override def musicAdded(
                             addition: Addition,
                             maybeTags: Option[Tags],
                             maybeArtwork: Option[Array[Byte]],
                             number: Int,
                             total: Int,
                             resource: DocumentFile): Unit = {
      sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, resource.getUri))
    }

    /**
      * Notify that synchronising has finished.
      * @param count The total number of changes.
      */
    override def synchronisingFinished(count: Int): Unit = {
      buildAndNotify(Left(Messages.Sync.finished(count)))
    }

    /**
      * Notify that synchronising has finished.
      * @param e The error to report.
      * @param maybeIdx The index of the change that failed, if any.
      */
    override def synchronisingFailed(e: Exception, maybeIdx: Option[Int]): Unit = {
      buildAndNotify(Left(Messages.Sync.failed(e, maybeIdx)))
    }
  }


  /**
    * Create a new notification and notify the notification manager.
    * @param data Either a failure message or a [[ChangeModel]]
    */
  def buildAndNotify(data: Either[String, ChangeModel]): Unit = {
    notificationManager.notify(ONGOING_NOTIFICATION_ID, buildNotification(data))
  }

  /**
    * Build a new [[Notification]].
    * @param data Either a failure string or a [[ChangeModel]].
    * @return A [[Notification]] that presents the data.
    */
  def buildNotification(data: Either[String, ChangeModel]): Notification = {
    val builder = new Notification.Builder(this).
      setContentTitle(Messages.Sync.notificationTitle).
      setSmallIcon(R.drawable.ic_action_sync)
    data match {
      case Left(message) =>
        builder.
          setContentText(message).
          setOngoing(false)
      case Right(ChangeModel(change, maybeTags, maybeArtwork, idx, total)) =>
        builder.
          setProgress(total, idx + 1, false).
          setOngoing(true)
        val lines: Seq[String] = maybeTags match {
          case Some(tags) =>
            Seq(tags.artist, tags.formattedAlbum, s"${tags.trackNumber} ${tags.title}")
          case None =>
            change match {
              case Addition(relativePath, _, _) => Seq(Messages.Sync.adding, relativePath.toString)
              case Removal(relativePath, _) => Seq(Messages.Sync.removing, relativePath.toString())
            }
        }
        builder.setLargeIcon(Artwork(maybeArtwork, iconWidth, iconHeight))
        val style = lines.foldLeft(new Notification.InboxStyle()) { (style, line) => style.addLine(line) }
        builder.setStyle(style)
    }
    builder.build()
  }
}
