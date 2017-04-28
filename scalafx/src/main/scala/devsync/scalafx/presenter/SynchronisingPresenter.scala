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

import java.net.URL
import java.nio.file.Path

import cats.data.EitherT
import cats.instances.future._
import devsync.json._
import devsync.scalafx.model.{SynchroniseAction, SynchroniseAddition, SynchroniseRemoval}
import devsync.scalafx.util.ObservableValues._
import devsync.scalafx.util.PathResource._
import devsync.scalafx.util.{ConstantProgressTask, Services, TaskUpdates}
import devsync.scalafx.view.SynchronisingView
import devsync.sync.DeviceListener

import scala.concurrent.ExecutionContext
import scalafx.beans.property.ObjectProperty
import scalafx.concurrent.Task
import scalafx.scene.Parent
import scalafx.scene.text.Font

/**
  * Created by alex on 14/04/17
  **/
case class SynchronisingPresenter(
                              currentPresenter: ObjectProperty[Option[Presenter]],
                              serverUrl: URL,
                              devicePath: Path,
                              deviceDescriptor: DeviceDescriptor)
                            (implicit executionContext: ExecutionContext, defaultFont: Font) extends Presenter {

  val synchronisingView = SynchronisingView()
  val initialisingTask: ConstantProgressTask[SynchroniseAction, Int] = ConstantProgressTask.fromFuture[SynchroniseAction, Int] { updates: TaskUpdates[SynchroniseAction, Int] =>
    val changesClient = Services.changesClient(serverUrl)
    val deviceListener = new DeviceListener[Path] {

      def action(maybeAction: Option[SynchroniseAction], number: Int, total: Int): Unit = {
        updates.updateProgress(number, total)
        updates.updateIntermediateValue(maybeAction)

      }

      override def addingMusic(addition: Addition, maybeTags: Option[Tags], maybeArtwork: Option[Array[Byte]], number: Int, total: Int): Unit = {
        val maybeSynchroniseAction = maybeTags.map { tags =>
          SynchroniseAddition(maybeArtwork, tags.artist, tags.formattedAlbum, tags.formattedTrack)
        }
        action(maybeSynchroniseAction, number, total)
      }

      override def removingMusic(removal: Removal, number: Int, total: Int): Unit = {
        action(Some(SynchroniseRemoval(removal.relativePath)), number, total)
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
    EitherT(Services.device.synchronise(devicePath, changesClient, deviceListener)).leftMap(_._1)
  }
  initialisingTask.intermediateValue.onAltered { maybeAction =>
    synchronisingView.action = maybeAction
  }
  initialisingTask.workDone.onAltered { workDone =>
    synchronisingView.workDone = workDone.longValue()
  }
  initialisingTask.totalWork.onAltered { totalWork =>
    synchronisingView.totalWork = totalWork.longValue()
  }

  override def content(): Parent = synchronisingView

  override def initialise(): Task[_] = initialisingTask
}
