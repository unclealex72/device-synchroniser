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

package devsync.scalafx

import java.io.ByteArrayOutputStream
import java.net.URL
import java.nio.file.{Path, Paths}
import java.util.concurrent.Executors

import cats.data.EitherT
import cats.instances.future._
import cats.syntax.either._
import com.typesafe.scalalogging.StrictLogging
import devsync.json._
import devsync.monads.FutureEither
import devsync.remote.ChangesClient
import devsync.scalafx.PathResource._
import devsync.sync.{DeviceListener, Progress}
import javafx.collections.ObservableList
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scalafx.Includes._
import scalafx.application.{JFXApp, Platform}
import scalafx.scene.{Parent, Scene}

import scala.util.{Failure, Success, Try}

/**
  * Main entry point for the Device Synchroniser Plus app.
  **/
object DeviceSynchroniserPlus extends JFXApp with StrictLogging {

  private val executorService = Executors.newFixedThreadPool(2)
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(executorService)

  private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm, EEE dd/MM/yyyy").withZone(ZoneId.systemDefault())

  stage = new JFXApp.PrimaryStage() {
    title = "Device Synchroniser+"

    val controllerAndView: ViewAndController[ChangelogController] = ChangelogController(this) {
      executorService.shutdown()
      this.close()
    }

    val controller: ChangelogController = controllerAndView.controller

    val view: Parent = controllerAndView.view
    scene = new Scene(view)

    onCloseRequest = handle {
      executorService.shutdownNow()
    }

    val discovery = for {
      _ <- ui {
        controller.messages("Searching")
      }
      synchronisingInformation <- loadSynchronisingInformation()
      _ <- ui {
        controller.messages("Loading changes")
      }
      changelogItems <- loadChangelogItems(synchronisingInformation)
      changelogItemModels <- loadChangelogItemModels(synchronisingInformation, changelogItems)
      _ <- ui {
        if (changelogItemModels.isEmpty) {
          controller.noChanges()
        }
        else {
          controller.changes()
          controller.changelogItems().addAll(changelogItemModels: _*)
          val topMessage: String = synchronisingInformation.deviceDescriptor.maybeLastModified match {
            case Some(at) => s"Last synchronised at ${formatter.format(at)}"
            case None => "Never synchronised"
          }
          val bottomMessage: String = if (changelogItemModels.size == 1) {
            "There is 1 change"
          }
          else {
            s"There are ${changelogItemModels.size} changes"
          }
          controller.messages(topMessage, bottomMessage)
          controller.synchronise(Future(synchronise(synchronisingInformation)))
        }
      }
    } yield {}

    discovery.recover {
      case ex: Exception => controller.error(ex)
    }

    def synchronise(
                     synchronisingInformation: SynchronisingInformation): Try[Unit] = {
      val changelogItemModels: ObservableList[ChangelogItemModel] = controller.changelogItems()
      val modelsByAlbumRelativePath: Map[RelativePath, Option[ChangelogItemModel]] =
        changelogItemModels.toSeq.groupBy(_.albumRelativePath).mapValues(_.headOption)
      val deviceListener: DeviceListener[Path] = new DeviceListener[Path] {

        var previousChangelogItemModel: Option[ChangelogItemModel] = None

        override def synchronisingStarting(): Unit = ui {
          controller.synchronising()
          controller.messages("Synchronising")
        }

        override def addingMusic(
                                  addition: Addition,
                                  maybeTags: Option[Tags],
                                  maybeArtwork: Option[Array[Byte]],
                                  overallProgress: Progress): Unit = adding(addition, maybeTags, -1)

        override def musicAdded(
                                 addition: Addition,
                                 maybeTags: Option[Tags],
                                 maybeArtwork: Option[Array[Byte]],
                                 overallProgress: Progress,
                                 resource: Path): Unit = adding(addition, maybeTags, 0)

        def adding(addition: Addition, maybeTags: Option[Tags], offset: Int): Unit = ui {
          for {
            albumRelativePath <- addition.relativePath.maybeParent
            tags <- maybeTags
            changelogItemModel <- modelsByAlbumRelativePath(albumRelativePath)
          } yield {
            if (changelogItemModels.indexOf(changelogItemModel) != 0) {
              changelogItemModels.remove(changelogItemModel)
              changelogItemModels.add(0, changelogItemModel)
            }
            val currentChangelogItemModel = Some(changelogItemModel)
            if (currentChangelogItemModel != previousChangelogItemModel) {
              previousChangelogItemModel.foreach(dispose)
            }
            else {
              val workDone: Int = tags.trackNumber + offset
              val totalWork: Int = tags.totalTracks
              changelogItemModel.progress.value = Some((workDone, totalWork))
            }
            previousChangelogItemModel = currentChangelogItemModel
          }
        }

        def remove(removal: Removal): Unit = ui {
          for {
            albumRelativePath <- removal.relativePath.maybeParent
            changelogItemModel <- modelsByAlbumRelativePath(albumRelativePath)
          } yield {
            dispose(changelogItemModel)
          }
        }

        def dispose(changelogItemModel: ChangelogItemModel): Unit = {
          changelogItemModels.remove(changelogItemModel)
        }

        override def removingMusic(removal: Removal, overallProgress: Progress): Unit = remove(removal)

        override def musicRemoved(removal: Removal, overallProgress: Progress): Unit = {}

        override def synchronisingFailed(e: Exception, maybeIdx: Option[Int]): Unit = ui {
          controller.error(e)
        }

        override def synchronisingFinished(count: Int): Unit = ui {
          controller.finished(count)
        }
      }
      Services.device.synchronise(
        synchronisingInformation.rootPath, synchronisingInformation.changesClient, deviceListener) match {
        case Right(_) => Success({})
        case Left((ex, _)) => Failure(ex)
      }
    }

    def ui(callback: => Unit): Try[Unit] = Try(Platform.runLater(callback))

    def loadSynchronisingInformation(): Try[SynchronisingInformation] = {
      val mediaDirectory: String = Option(System.getenv("MEDIA_DIRECTORY")).getOrElse("/media")
      for {
        url <- Services.flacManagerDiscovery.discover(Option(System.getenv("FLAC_DEV")).isDefined, 30.seconds)
        deviceDescriptorAndPath <- Services.deviceDiscoverer.discover(Paths.get(mediaDirectory, System.getProperty("user.name")), 2)
      } yield {
        SynchronisingInformation(url, deviceDescriptorAndPath._1, deviceDescriptorAndPath._2)
      }
    }

    def loadChangelogItems(synchronisingInformation: SynchronisingInformation): Try[Seq[ChangelogItem]] = {
      val deviceDescriptor: DeviceDescriptor = synchronisingInformation.deviceDescriptor
      synchronisingInformation.changesClient.
        changelogSince(deviceDescriptor.user, deviceDescriptor.extension, deviceDescriptor.maybeLastModified).map(_.items)
    }

    def loadChangelogItemModels(
                                 synchronisingInformation: SynchronisingInformation,
                                 changelogItems: Seq[ChangelogItem]): Try[Seq[ChangelogItemModel]] = {
      def generateModel(changelogItem: ChangelogItem): ChangelogItemModel = {
        val changesClient: ChangesClient = synchronisingInformation.changesClient
        val maybeTags: Option[Tags] = changesClient.tags(changelogItem).toOption
        val maybeArtwork: Option[Array[Byte]] = {
          val out = new ByteArrayOutputStream()
          changesClient.artwork(changelogItem, out).map(_ => out.toByteArray).toOption
        }
        ChangelogItemModel(changelogItem.at, maybeArtwork, maybeTags, changelogItem.parentRelativePath)
      }

      Try(changelogItems.map(generateModel))
    }

    case class SynchronisingInformation(serverUrl: URL, deviceDescriptor: DeviceDescriptor, rootPath: Path) {
      val changesClient: ChangesClient = Services.changesClient(serverUrl)
    }

  }
}
