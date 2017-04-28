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

import devsync.json.{IsoDate, RelativePath}
import devsync.scalafx.model.ChangelogItemModel
import devsync.scalafx.util.Artwork

import scalafx.beans.property.ReadOnlyObjectProperty
import scalafx.geometry.Pos
import scalafx.scene.Node
import scalafx.scene.control.Label
import scalafx.scene.image.ImageView
import scalafx.scene.layout.{HBox, VBox}
import scalafx.scene.text.Font

/**
  * Created by alex on 14/04/17
  **/
trait ChangelogItemView {

  def model: ReadOnlyObjectProperty[ChangelogItemModel]

}

object ChangelogItemView {

  private val defaultHeight: Double = 96

  def apply(_model: ChangelogItemModel)(implicit defaultFont: Font): Node with ChangelogItemView = {
    val modelProperty = new ReadOnlyObjectProperty[ChangelogItemModel](this, "model", _model)
    val artwork = Artwork(_model.maybeArtwork)
    val texts = _model.relativePathOrAlbum match {
      case Left(relativePath) => Seq(relativePath.toString)
      case Right(album) => Seq(album.artist, album.album)
    }
    val textBoxes = new VBox {
      children = (texts :+ _model.at.format("EEE dd/MM/yyyy HH:mm")).map { text =>
        new Label(text) {
          font = defaultFont
        }
      }
      alignment = Pos.CenterLeft
    }

    val image = new ImageView(artwork) {
      fitWidth = defaultHeight
      fitHeight = defaultHeight
    }
    new HBox(10d, image, textBoxes) with ChangelogItemView {
      prefHeight = defaultHeight

      override def model: ReadOnlyObjectProperty[ChangelogItemModel] = modelProperty
    }
  }
}