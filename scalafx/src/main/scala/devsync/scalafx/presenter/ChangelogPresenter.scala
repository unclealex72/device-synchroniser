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

package devsync.scalafx.presenter

import java.io.ByteArrayOutputStream
import java.net.URL
import java.nio.file.Path

import cats.data.EitherT
import cats.instances.future._
import cats.syntax.either._
import devsync.json.DeviceDescriptor
import devsync.remote.ChangesClient
import devsync.scalafx.model.{Album, ChangelogItemModel}
import devsync.scalafx.util.ObservableValues._
import devsync.scalafx.util.{ConstantProgressTask, Services, TaskUpdates}
import devsync.scalafx.view.DeviceInformationView

import scala.concurrent.{ExecutionContext, Future}
import scalafx.beans.property.ObjectProperty
import scalafx.collections.ObservableSet
import scalafx.concurrent.Task
import scalafx.scene.Parent
import scalafx.scene.text.Font
import scalafx.Includes._

/**
  * A presenter that shows all the changelog items.
  * @param currentPresenter The current presenter property.
  * @param serverUrl The URL of the Flac Manager server.
  * @param devicePath The path of the previously found device.
  * @param deviceDescriptor The device descriptor for the previously found device.
  * @param executionContext An execution context used to execute asynchronous tasks.
  * @param defaultFont The default font to use.
  */
case class ChangelogPresenter(
                               currentPresenter: ObjectProperty[Option[Presenter]],
                               serverUrl: URL,
                               devicePath: Path,
                               deviceDescriptor: DeviceDescriptor)
                             (implicit executionContext: ExecutionContext, defaultFont: Font) extends Presenter {

  private val items: ObservableSet[ChangelogItemModel] = ObservableSet.empty[ChangelogItemModel]

  private val deviceInformationView =
    DeviceInformationView(
      deviceDescriptor.user,
      devicePath,
      deviceDescriptor.maybeLastModified,
      items,
      transition(Some(SynchronisingPresenter(currentPresenter, serverUrl, devicePath, deviceDescriptor)), currentPresenter))

  private val changesClient: ChangesClient = Services.changesClient(serverUrl)

  private val changelogTask: ConstantProgressTask[ChangelogItemModel, Int] =
    ConstantProgressTask.fromFuture[ChangelogItemModel, Int] { updates: TaskUpdates[ChangelogItemModel, Int] =>
    EitherT(Future(changesClient.changelogSince(deviceDescriptor.user, deviceDescriptor.maybeLastModified))).map { changelog =>
      changelog.items.foreach { item =>
        val eventualMaybeArtwork = Future {
          val by = new ByteArrayOutputStream()
          changesClient.artwork(item, by).toOption.map(_ => by.toByteArray)
        }
        val eventualRelativePathOrAlbum = Future {
          changesClient.tags(item) match {
            case Right(tags) => Right(Album(tags.album, tags.artist))
            case _ => Left(item.relativePath)
          }
        }
        for {
          maybeArtwork <- eventualMaybeArtwork
          description <- eventualRelativePathOrAlbum
        } yield {
          updates.updateIntermediateValue(Some(ChangelogItemModel(item.at, maybeArtwork, description)))
        }
      }
      changelog.items.length
    }
  }

  changelogTask.intermediateValue.onAltered { maybeItem =>
    items ++= maybeItem
  }
  changelogTask.onSucceeded = handle {
    deviceInformationView.ready = true
  }

  /**
    * @inheritdoc
    */
  def content(): Parent = deviceInformationView


  /**
    * @inheritdoc
    */
  def initialise(): Task[_] = changelogTask
}
