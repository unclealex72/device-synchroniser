package uk.co.unclealex.devsync

import java.net.URL

import android.app._
import android.content.{Context, Intent}
import android.net.Uri
import android.support.v4.provider.DocumentFile
import com.typesafe.scalalogging.StrictLogging
import devsync.common.Messages
import devsync.json._
import devsync.sync.DeviceListener
import macroid.Contexts
import uk.co.unclealex.devsync.Async._
import uk.co.unclealex.devsync.DocumentFileResource._
import uk.co.unclealex.devsync.IntentHelper._

/**
  * Created by alex on 02/04/17
  **/
class SynchroniseService extends IntentService("devsync") with Contexts[Service] with StrictLogging {

  lazy val notificationManager: NotificationManager =
    getSystemService(Context.NOTIFICATION_SERVICE).asInstanceOf[NotificationManager]

  lazy val iconWidth: Int = getResources.getDimensionPixelSize(android.R.dimen.notification_large_icon_width)
  lazy val iconHeight: Int = getResources.getDimensionPixelSize(android.R.dimen.notification_large_icon_height)

  val ONGOING_NOTIFICATION_ID = 1

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

  def createNewDeviceListener: DeviceListener[DocumentFile] = new DeviceListener[DocumentFile] {

    override def synchronisingStarting(): Unit = {
      val notification = buildNotification(Left("Starting"))
      startForeground(ONGOING_NOTIFICATION_ID, notification)
      notificationManager.notify(ONGOING_NOTIFICATION_ID, notification)
    }

    override def removingMusic(removal: Removal, number: Int, total: Int): Unit = {
      buildAndNotify(Right((removal, None, None, number, total)))
    }

    override def musicRemoved(removal: Removal, number: Int, total: Int): Unit = {}

    override def addingMusic(addition: Addition,
                             maybeTags: Option[Tags],
                             maybeArtwork: Option[Array[Byte]], number: Int, total: Int): Unit = {
      buildAndNotify(Right((addition, maybeTags, maybeArtwork, number, total)))
    }

    override def musicAdded(
                             addition: Addition,
                             maybeTags: Option[Tags],
                             maybeArtwork: Option[Array[Byte]],
                             number: Int,
                             total: Int,
                             resource: DocumentFile): Unit = {
      sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, resource.getUri))
    }

    override def synchronisingFinished(count: Int): Unit = {
      buildAndNotify(Left(Messages.Sync.finished(count)))
    }

    override def synchronisingFailed(e: Exception, maybeIdx: Option[Int]): Unit = {
      buildAndNotify(Left(Messages.Sync.failed(e, maybeIdx)))
    }
  }

  def buildAndNotify(data: Either[String, (Change, Option[Tags], Option[Array[Byte]], Int, Int)]): Unit = {
    notificationManager.notify(ONGOING_NOTIFICATION_ID, buildNotification(data))
  }

  def buildNotification(data: Either[String, (Change, Option[Tags], Option[Array[Byte]], Int, Int)]): Notification = {
    val builder = new Notification.Builder(this).
      setContentTitle(Messages.Sync.notificationTitle).
      setSmallIcon(R.drawable.ic_action_sync)
    data match {
      case Left(message) =>
        builder.
          setContentText(message).
          setOngoing(false)
      case Right((change, maybeTags, maybeArtwork, idx, total)) =>
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
