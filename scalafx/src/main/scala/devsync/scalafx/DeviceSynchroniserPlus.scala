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
import devsync.remote.ChangesClient
import devsync.scalafx.PathResource._
import devsync.sync.DeviceListener

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scalafx.Includes._
import scalafx.application.{JFXApp, Platform}
import scalafx.scene.{Parent, Scene}

/**
  * Main entry point for the Device Synchroniser Plus app.
  **/
object DeviceSynchroniserPlus extends JFXApp with StrictLogging {

  private val executorService = Executors.newFixedThreadPool(2)
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(executorService)

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
          val topMessage = synchronisingInformation.deviceDescriptor.maybeLastModified match {
            case Some(at) => s"Last synchronised at ${at.format("HH:mm, EEE dd/MM/YYYY")}"
            case None => "Never synchronised"
          }
          val bottomMessage = if (changelogItemModels.size == 1) {
            "There is 1 change"
          }
          else {
            s"There are ${changelogItemModels.size} changes"
          }
          controller.messages(topMessage, bottomMessage)
          controller.synchronise(synchronise(synchronisingInformation))
        }
      }
    } yield {}

    discovery.leftMap { ex =>
      ui {
        controller.error(ex)
      }
    }

    def synchronise(
                     synchronisingInformation: SynchronisingInformation): EitherT[Future, Exception, Unit] = {
      val changelogItemModels = controller.changelogItems()
      val modelsByAlbumRelativePath: Map[RelativePath, Option[ChangelogItemModel]] =
        changelogItemModels.toSeq.groupBy(_.albumRelativePath).mapValues(_.headOption)
      val deviceListener = new DeviceListener[Path] {

        var previousChangelogItemModel: Option[ChangelogItemModel] = None

        override def synchronisingStarting(): Unit = ui {
          controller.synchronising()
          controller.messages("Synchronising")
        }

        override def addingMusic(
                                  addition: Addition,
                                  maybeTags: Option[Tags],
                                  maybeArtwork: Option[Array[Byte]],
                                  number: Int,
                                  total: Int): Unit = adding(addition, maybeTags, -1)

        override def musicAdded(
                                 addition: Addition,
                                 maybeTags: Option[Tags],
                                 maybeArtwork: Option[Array[Byte]],
                                 number: Int,
                                 total: Int,
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
              val workDone = tags.trackNumber + offset
              val totalWork = tags.totalTracks
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

        override def removingMusic(removal: Removal, number: Int, total: Int): Unit = remove(removal)

        override def musicRemoved(removal: Removal, number: Int, total: Int): Unit = {}

        override def synchronisingFailed(e: Exception, maybeIdx: Option[Int]): Unit = ui {
          controller.error(e)
        }

        override def synchronisingFinished(count: Int): Unit = ui {
          controller.finished(count)
        }
      }
      EitherT(Services.device.synchronise(
        synchronisingInformation.rootPath, synchronisingInformation.changesClient, deviceListener)).leftMap(_._1).map(_ => {})
    }

    def ui(callback: => Unit): EitherT[Future, Exception, Unit] = EitherT.right {
      Future.successful(Platform.runLater(callback))
    }

    def loadSynchronisingInformation(): EitherT[Future, Exception, SynchronisingInformation] = {
      for {
        url <- Services.flacManagerDiscovery.discover(Option(System.getenv("FLAC_DEV")).isDefined, 30.seconds)
        deviceDescriptorAndPath <- Services.deviceDiscoverer.discover(Paths.get("/media", System.getProperty("user.name")), 2)
      } yield {
        SynchronisingInformation(url, deviceDescriptorAndPath._1, deviceDescriptorAndPath._2)
      }
    }

    def loadChangelogItems(synchronisingInformation: SynchronisingInformation): EitherT[Future, Exception, Seq[ChangelogItem]] = {
      for {
        changelog <- EitherT {
          Future {
            val deviceDescriptor = synchronisingInformation.deviceDescriptor
            synchronisingInformation.changesClient.
              changelogSince(deviceDescriptor.user, deviceDescriptor.maybeLastModified)
          }
        }
      } yield {
        changelog.items
      }
    }

    def loadChangelogItemModels(
                                 synchronisingInformation: SynchronisingInformation,
                                 changelogItems: Seq[ChangelogItem]): EitherT[Future, Exception, Seq[ChangelogItemModel]] = {
      def generateModel(changelogItem: ChangelogItem): Future[ChangelogItemModel] = {
        val changesClient = synchronisingInformation.changesClient
        val eventualMaybeTags = Future(changesClient.tags(changelogItem).toOption)
        val eventualMaybeArtwork = Future {
          val out = new ByteArrayOutputStream()
          changesClient.artwork(changelogItem, out).map(_ => out.toByteArray).toOption
        }
        for {
          maybeTags <- eventualMaybeTags
          maybeArtwork <- eventualMaybeArtwork
        } yield {
          ChangelogItemModel(changelogItem.at, maybeArtwork, maybeTags, changelogItem.parentRelativePath)
        }
      }

      val eventualChangelogItemModels: Future[Seq[ChangelogItemModel]] = Future.sequence(changelogItems.map(generateModel))
      EitherT.right(eventualChangelogItemModels)
    }

    case class SynchronisingInformation(serverUrl: URL, deviceDescriptor: DeviceDescriptor, rootPath: Path) {
      val changesClient: ChangesClient = Services.changesClient(serverUrl)
    }

  }
}
