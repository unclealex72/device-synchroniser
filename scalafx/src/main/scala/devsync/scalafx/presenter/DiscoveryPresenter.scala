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

import java.nio.file.Paths

import cats.instances.future._
import devsync.scalafx.util.{ConstantProgressTask, Services, TaskUpdates}
import devsync.scalafx.view.DiscoveryView

import scala.concurrent.ExecutionContext
import scalafx.beans.property.ObjectProperty
import scalafx.concurrent.Task
import scalafx.scene.{Node, Parent}
import scalafx.scene.text.Font

/**
  * The presenter used to discover the location of the Flac Manager server and the location of a user's device.
  * @param currentPresenter The current presenter property.
  * @param executionContext An execution context used to execute asynchronous tasks.
  * @param defaultFont The default font to use.
  */
case class DiscoveryPresenter(currentPresenter: ObjectProperty[Option[Presenter]])
                             (implicit executionContext: ExecutionContext, defaultFont: Font) extends Presenter {

  private val discoveryView = DiscoveryView()

  private val initialisingTask: ConstantProgressTask[Unit, Unit] = ConstantProgressTask.fromFuture[Unit, Unit] { (updater: TaskUpdates[Unit, Unit]) =>
    for {
      _ <- updater.updateMessageF("Searching for a Flac Manager server")
      url <- Services.flacManagerDiscovery.discover(Option(System.getenv("FLAC_DEV")).isDefined)
      _ <- updater.updateMessageF("Searching for a device")
      deviceDescriptorAndPath <- Services.deviceDiscoverer.discover(Paths.get("/media", System.getProperty("user.name")), 2)
    } yield {
      transition(Some(ChangelogPresenter(currentPresenter, url, deviceDescriptorAndPath._2, deviceDescriptorAndPath._1)), currentPresenter)
    }
  }

  discoveryView.message <== initialisingTask.message

  /**
    * @inheritdoc
    */
  def content(): Parent = discoveryView

  /**
    * @inheritdoc
    */
  def initialise(): Task[_] = initialisingTask

}
