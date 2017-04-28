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

import scalafx.application.JFXApp
import scalafx.beans.property.StringProperty
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.Parent
import scalafx.scene.control.{Label, ProgressIndicator}
import scalafx.scene.layout.BorderPane
import scalafx.scene.text.Font

/**
  * Created by alex on 14/04/17
  **/
trait DiscoveryView {

  def message: StringProperty
  def message_=(v: String) {
    message() = v
  }

}

object DiscoveryView {

  private lazy val defaultPadding: Insets = Insets(5, 0, 5, 0)

  def apply()(implicit defaultFont: Font): Parent with DiscoveryView = {
    val messageLabel: Label = new Label {
      padding = defaultPadding
      maxWidth = Double.MaxValue
      alignment = Pos.Center
      font = defaultFont
    }

    val progressIndicator: ProgressIndicator = new ProgressIndicator {
      padding = defaultPadding
      maxWidth = 200
      maxHeight = 200
    }
    new BorderPane with DiscoveryView {
      val message: StringProperty = messageLabel.text
      center = progressIndicator
      bottom = messageLabel
      padding = defaultPadding
    }
  }
}