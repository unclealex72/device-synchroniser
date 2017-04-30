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

package devsync.scalafx.view

import java.nio.file.Path

import devsync.json.IsoDate
import devsync.scalafx.model.ChangelogItemModel

import scalafx.collections.ObservableSet
import scalafx.scene.{Node, Parent}
import scalafx.scene.control.{Button, Label, ScrollPane}
import scalafx.scene.layout.BorderPane
import scalafx.scene.text.Font
import scalafx.Includes._
import scalafx.beans.property.BooleanProperty
import scalafx.geometry.Insets
import devsync.scalafx.util.ObservableValues._

/**
  * The view for showing device information.
  **/
trait DeviceInformationView {

  /**
    * Get the ready property.
    * @return The ready property.
    */
  def ready: BooleanProperty

  /**
    * Set the ready property.
    * @param ready The new value of the ready property.
    */
  def ready_=(ready: Boolean): Unit = {
    this.ready() = ready
  }
}

/**
  * Create a new [[DeviceInformationView]]
  */
object DeviceInformationView {

  /**
    * Create a new [[DeviceInformationView]]
    * @param user The user who own's the device.
    * @param devicePath The location of the device.
    * @param maybeLastUpdated The last time the device was last updated or none.
    * @param items A set of changelog items.
    * @param synchroniseCallback The synchronising code.
    * @param defaultFont The default font to use.
    * @return A new [[DeviceInformationView]]
    */
  def apply(user: String,
            devicePath: Path,
            maybeLastUpdated: Option[IsoDate],
            items: ObservableSet[ChangelogItemModel],
            synchroniseCallback: => Unit)
           (implicit defaultFont: Font): Parent with DeviceInformationView = {
    val synchroniseButton = new Button("No changes") {
      disable = true
      onAction = handle(synchroniseCallback)
      font = defaultFont
    }
    val _ready = BooleanProperty(false)
    items.onChange {
      val count = items.size
      synchroniseButton.text = if (count == 0) "No Changes" else s"Synchronise $count change${if (count != 1) "s" else ""}"
    }
    _ready.onAltered { ready =>
      synchroniseButton.disable = !ready || items.size == 0
    }
    new BorderPane with DeviceInformationView {
      top = new Label(
        Seq(
          s"Found $user's device at $devicePath.",
          maybeLastUpdated match {
            case Some(lastUpdated) => s"It was last updated on ${lastUpdated.format("EEEE dd MMM yyy HH:mm")}."
            case None => "It has never been updated."
          }).mkString(" ")) {
        font = defaultFont
      }
      center = new ScrollPane {
        content = ChangelogView(items)
      }
      bottom = Anchor.newRight(synchroniseButton)
      padding = Insets(10, 10, 10, 10)

      def ready: BooleanProperty = _ready
    }
  }
}