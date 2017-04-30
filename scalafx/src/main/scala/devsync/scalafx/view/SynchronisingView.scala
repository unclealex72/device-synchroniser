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

import devsync.scalafx.model.{SynchroniseAction, SynchroniseAddition, SynchroniseRemoval}
import devsync.scalafx.util.Artwork
import devsync.scalafx.util.ObservableValues._

import scalafx.beans.property.{LongProperty, ObjectProperty}
import scalafx.geometry.{Dimension2D, Insets, Pos}
import scalafx.scene.{Node, Parent}
import scalafx.scene.control.Label
import scalafx.scene.image.ImageView
import scalafx.scene.layout.{AnchorPane, HBox, VBox}
import scalafx.scene.text.Font

/**
  * The view to show whilst synchronising.
  **/
trait SynchronisingView extends Dimensions {

  /**
    * The requested dimensions for the stage.
    */
  val dimensions: Dimension2D = new Dimension2D(width = 800, height = 155)

  /**
    * Get the current synchronisation action, if any.
    * @return The synchronisation action property.
    */
  def action: ObjectProperty[Option[SynchroniseAction]]

  /**
    * Set the current synchronisation action, if any.
    * @param sa The new value of the synchronisation action.
    */
  def action_=(sa: Option[SynchroniseAction]): Unit = {
    action() = sa
  }

  /**
    * Get the work done property.
    * @return The work done property.
    */
  def workDone: LongProperty

  /**
    * Set the amount of work done.
    * @param workDone New new amount of work done.
    */
  def workDone_=(workDone: Long): Unit = {
    this.workDone() = workDone
  }

  /**
    * Get the total work property.
    * @return The total work property.
    */
  def totalWork: LongProperty

  /**
    * Set the amount of total work.
    * @param totalWork New new amount of total work.
    */
  def totalWork_=(totalWork: Long): Unit = {
    this.totalWork() = totalWork

  }
}

/**
  * Create a new [[SynchronisingView]].
  */
object SynchronisingView {

  private val defaultHeight: Double = 128
  private val defaultWidth: Double = 600

  /**
    * Create a new [[SynchronisingView]].
    * @param defaultFont The default font to use.
    * @return A new [[SynchronisingView]]
    */
  def apply()(implicit defaultFont: Font): Parent with SynchronisingView = {
    val _action: ObjectProperty[Option[SynchroniseAction]] = ObjectProperty(None)
    val progressBar: AnnotatedProgressBar = new AnnotatedProgressBar {
      prefWidth = defaultWidth
    }

    val textBoxes = new VBox {
      alignment = Pos.BottomLeft
    }
    val allBoxes = new VBox {
      children = Seq(textBoxes, progressBar)
      prefWidth = Double.MaxValue
    }

    val image = new ImageView {
      fitWidth = defaultHeight
      fitHeight = defaultHeight
    }
    _action.onAltered { maybeAction =>
      val (artwork, texts) = maybeAction match {
        case Some(SynchroniseAddition(maybeArtwork, artist, album, track)) =>
          (Artwork(maybeArtwork), Seq(artist, album, track))
        case Some(SynchroniseRemoval(relativePath)) =>
          (Artwork.empty, Seq(relativePath.toString))
        case None => (Artwork.empty, Seq.empty)
      }
      image.image = artwork
      textBoxes.children = texts.map { txt =>
        new Label(txt) {
          font = defaultFont
          padding = Insets(5, 0, 5, 0)
        }
      }
    }
    val anchoredBoxes = new AnchorPane {
      prefWidth = defaultWidth
      children = Seq(Anchor.fill(allBoxes))
    }

    new HBox(10d, image, anchoredBoxes) with SynchronisingView {
      prefHeight = defaultHeight
      override def workDone: LongProperty = progressBar.workDone

      override def totalWork: LongProperty = progressBar.totalWork

      override def action: ObjectProperty[Option[SynchroniseAction]] = _action
    }
  }
} 