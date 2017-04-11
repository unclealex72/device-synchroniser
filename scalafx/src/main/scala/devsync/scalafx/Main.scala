package devsync.scalafx

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.net.URL
import java.nio.file.{Path, Paths}
import java.util.concurrent.Executors

import cats.data.EitherT
import cats.syntax.either._
import cats.instances.future._
import com.typesafe.scalalogging.StrictLogging
import devsync.scalafx.PathResource._
import devsync.json._
import devsync.remote.ChangesClient
import devsync.sync.{DeviceListener, IO}

import scalafx.scene.control._
import scalafx.Includes._
import scala.concurrent.{ExecutionContext, Future}
import scalafx.application.{JFXApp, Platform}
import scalafx.application.JFXApp.PrimaryStage
import scalafx.geometry.Insets
import scalafx.scene.Scene
import scalafx.scene.control._
import scalafx.scene.layout._
import scala.concurrent.ExecutionContext.Implicits.global
import scalafx.beans.property.{ObjectProperty, ReadOnlyObjectProperty, ReadOnlyStringProperty, StringProperty}
import scalafx.collections.ObservableBuffer
import scalafx.concurrent.{Task, WorkerStateEvent}
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.image.{Image, ImageView}
import scalafx.stage.{Modality, Stage}

object Main extends JFXApp with StrictLogging {

  val device = Services.device
  val username = System.getProperty("user.name")

  implicit class EitherTImplicits[V](eitherT: EitherT[Future, Exception, V]) {
    def logging(msgBuilder: V => String): EitherT[Future, Exception, V] = {
      eitherT.map { v =>
        logger.info(msgBuilder(v))
        v
      }
    }
    def withNoIndex: EitherT[Future, (Exception, Option[Int]), V] = {
      eitherT.leftMap(e => (e, None))
    }
  }

  def initialise() : Unit = {
    val startUpWorker: Task[(URL, Path, DeviceDescriptor)] = Tasks.fromFuture {
      for {
        serverUrl <- findServerUrl()
        (deviceDescriptorAndRoot) <- findDeviceDescriptor()
      } yield {
        (serverUrl, deviceDescriptorAndRoot._2, deviceDescriptorAndRoot._1)
      }
    }

    startUpWorker.onFailed = { (_: WorkerStateEvent) =>
      new Alert(AlertType.Error) {
        initOwner(stage)
        title = "Cannot Initialise"
        headerText = "Either the server is not running or no USB device is plugged in."
        contentText = "Try again?"
        // Note that we override here default dialog buttons, OK and Cancel, with new ones.
        buttonTypes = Seq(ButtonType.Yes, ButtonType.No)
      }.showAndWait() match {
        case Some(ButtonType.Yes) =>
          initialise()
        case _ =>
          stage.close()
      }
    }
    startUpWorker.value.onChange { (_, _, serverUrlRootAndDeviceDescriptor) =>
      val (serverUrl, root, deviceDescriptor) = serverUrlRootAndDeviceDescriptor
      generateGui(serverUrl, root, deviceDescriptor)
    }
    Future { startUpWorker.run() }
  }

  stage = new PrimaryStage {
    title = "Device Synchroniser+"
    width = 800
    height = 600
  }

  /**
    * Show an indeterminate progress bar whilst looking for a device and Flac Manager server
    */
  stage.scene = new Scene {
    root = Anchor.fill {
      new BorderPane {
        center = new ProgressBar {
          maxWidth = 200
        }
      }
    }
  }

  initialise()

  def generateGui(url: URL, root: Path, deviceDescriptor: DeviceDescriptor): Unit = {

    val changesClient = Services.changesClient(url)
    val device = Services.device

    val synchroniseButton = new Button("Synchronise") {

      onAction = handle {
        synchronise(url, root, deviceDescriptor)
      }
      disable = true
    }

    val changelogCount = new Label {
    }

    val changelogItems = new ObservableBuffer[ReadOnlyChangelogItem]()

    val changelogTask = Tasks.fromFuture[Unit] {
      EitherT(Future(changesClient.changelogSince(deviceDescriptor.user, deviceDescriptor.maybeLastModified))).map { changelog =>
        changelog.items.foreach { item =>
          val eventualMaybeArtwork = Future {
            val by = new ByteArrayOutputStream()
            changesClient.artwork(item, by).toOption.map(_ => by.toByteArray)
          }
          val modifiedString = item.at.format("MMM dd/MM/yyyy HH:mm")
          val eventualDescription = Future {
            changesClient.tags(item) match {
              case Right(tags) => Seq(tags.artist, tags.album, modifiedString)
              case _ => Seq(item.relativePath.toString, modifiedString)
            }
          }
          for {
            maybeArtwork <- eventualMaybeArtwork
            description <- eventualDescription
          } yield {
            Platform.runLater(changelogItems += ReadOnlyChangelogItem(maybeArtwork, description, item.at))
          }
        }
      }
    }

    changelogItems.onChange { (newChangelogItems, _) =>
      val (disableSyncButton, countText) = newChangelogItems.size match {
        case 0 => (true, "There are no changes")
        case 1 => (false, "There is 1 change")
        case x => (false, s"There are $x changes")
      }
      synchroniseButton.disable = disableSyncButton
      changelogCount.text = countText
    }

    val lastUpdated = new Label {
      text = deviceDescriptor.maybeLastModified match {
        case Some(lastModified) => s"Last updated on ${lastModified.format("EEE dd/MM/yyyy HH:mm:ss")}"
        case _ => "This device has never been updated."
      }
    }

    val topPane = new VBox {
      children = Seq(lastUpdated, changelogCount)
    }
    val actionButtonBar = Anchor.right {
      new HBox {
        spacing = 10
        padding = Insets(10)
        children = Seq(synchroniseButton)
      }
    }
    stage.scene = new Scene {
      root = Anchor.fill {
        new BorderPane {
          top = topPane
          center = Anchor.fill(ChangelogTable(changelogItems))
          bottom = actionButtonBar
        }
      }
    }

    Future { changelogTask.run() }


  }

  /*
  val synchroniseResultAction = for {
    root <- findDeviceDescriptor.withNoIndex
    changesClient <- createChangesClient.withNoIndex
    synchroniseResult <- synchronise(root, changesClient)
  } yield synchroniseResult
  val errorLoggingSynchroniseResultAction = synchroniseResultAction.leftMap {
    case (e, maybeIdx) =>
      logger.error(s"Synchronising failed at index $maybeIdx", e)
  }
  Await.result(errorLoggingSynchroniseResultAction.value, Duration.Inf)
  executor.shutdown()
*/
  def findServerUrl(): EitherT[Future, Exception, URL] = {
    EitherT.right[Future, Exception, URL](Services.flacManagerDiscovery.discover).
      logging(url => s"Found Flac Manager at $url")
  }

  def findDeviceDescriptor(): EitherT[Future, Exception, (DeviceDescriptor, Path)] = {
    EitherT(Future(new DeviceDiscovererImpl(device).discover(Paths.get(s"/media/$username"), 2)))
  }.logging {
    case (deviceDescriptor, path) => Seq(
      s"Found a device for user ${deviceDescriptor.user} at $path",
      s"Last modified at ${deviceDescriptor.maybeLastModified}",
      s"Offset: ${deviceDescriptor.maybeOffset}"
    ).mkString("\n")
  }

  def synchronise(url: URL, root: Path, deviceDescriptor: DeviceDescriptor): Unit = {
    case class Track(_maybeArtwork: Option[Array[Byte]] = None, _description: Seq[String] = Seq.empty) {
      val maybeArtwork = new ReadOnlyObjectProperty[Option[Array[Byte]]](this, "maybeArtwork", _maybeArtwork)
      val description = new ReadOnlyObjectProperty[Seq[String]](this, "description", _description)
    }

    val currentTrack = ObjectProperty(Track())

    val synchroniseTask = Tasks.fromFuture[Int] { (task: TaskUpdates[Int]) =>
      val changesClient = Services.changesClient(url)
      val deviceListener = new DeviceListener[Path] {

        def track(relativePath: RelativePath, maybeDescription: Option[Seq[String]], maybeArtwork: Option[Array[Byte]], number: Int, total: Int): Unit = {
          task.updateProgress(number, total)
          val description = maybeDescription.getOrElse(Seq(relativePath.toString))
          Platform.runLater {
            currentTrack.set(Track(maybeArtwork, description))
          }
        }

        override def addingMusic(addition: Addition, maybeTags: Option[Tags], maybeArtwork: Option[Array[Byte]], number: Int, total: Int): Unit = {
          track(
            addition.relativePath,
            maybeTags.map(tags => Seq(tags.artist, tags.formattedAlbum, tags.formattedTrack)),
            maybeArtwork,
            number,
            total)
        }
        override def removingMusic(removal: Removal, number: Int, total: Int): Unit = {
          track(removal.relativePath, None, None, number, total)
        }

        override def musicAdded(addition: Addition, maybeTags: Option[Tags], maybeArtwork: Option[Array[Byte]], number: Int, total: Int, resource: Path): Unit = {
          addingMusic(addition, maybeTags, maybeArtwork, number + 1, total)
        }
        override def musicRemoved(removal: Removal, number: Int, total: Int): Unit = {
          removingMusic(removal, number + 1, total)
        }
        override def synchronisingStarting(): Unit = {}
        override def synchronisingFailed(e: Exception, maybeIdx: Option[Int]): Unit = {}
        override def synchronisingFinished(count: Int): Unit = {}
      }
      EitherT(device.synchronise(root, changesClient, deviceListener)).leftMap(_._1)
    }
    val artworkView = new ImageView {
      image = Artwork(None)
    }
    val currentTrackLabel = new Label("A")

    val progressBar = new ProgressBar {
      progress <== synchroniseTask.progress
    }
    val infoBox = new VBox {
      children = Seq(currentTrackLabel, progressBar)
    }
    val modalStage = new Stage {
      width = 640
      height = 640
      scene = new Scene {
        content = new HBox {
          children = Seq(artworkView, infoBox)
        }
      }
    }
    modalStage.initModality(Modality.ApplicationModal)
    modalStage.initOwner(stage)
    modalStage.show

    currentTrack.onChange { (_, _, currentTrack) =>
      artworkView.image = Artwork(currentTrack._maybeArtwork)
      currentTrackLabel.text = currentTrack._description.mkString("\n")
    }

    Future {
      synchroniseTask.run()
    }
  }
}