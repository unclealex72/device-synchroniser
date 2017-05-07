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

import javafx.scene.{control => jfxc, image => jfxi, layout => jfxl}

import devsync.scalafx.ObservableValues._
import devsync.scalafx.View._

import scalafx.Includes._
import scalafx.scene.control.{Label, ProgressBar}
import scalafx.scene.image.ImageView
import scalafx.scene.layout.Pane

/**
  * A controller that controls a single changelog item. As all manipulation of a changelog item can be made by
  * changing its model this controller needs no extra methods.
  */
trait ChangelogItemController {

}

/**
  * Create a new [[ChangelogItemController]] and it's associated FXML view.
  */
object ChangelogItemController {

  /**
    * Create a new [[ChangelogItemController]] and it's associated FXML view.
    * @param changelogItemModel The model for this controller and view.
    * @return A controller and view for the given model.
    */
  def apply(changelogItemModel: ChangelogItemModel): ViewAndController[ChangelogItemController] = {
    val view = View("changelog-item")
    val artworkView = view.component[jfxi.ImageView]("artworkView")
    val artistLabel = view.component[jfxc.Label]("artistLabel")
    val albumLabel = view.component[jfxc.Label]("albumLabel")
    val updatedLabel = view.component[jfxc.Label]("updatedLabel")
    val updatedPane = view.component[jfxl.Pane]("updatedPane")
    val progressPane = view.component[jfxl.Pane]("progressPane")
    val progressBar = view.component[jfxc.ProgressBar]("progressBar")
    view.withController[ChangelogItemController](
      new ChangelogItemControllerImpl(
        changelogItemModel, artworkView, artistLabel, albumLabel,
        updatedLabel, updatedPane, progressPane, progressBar))
  }
}

/**
  * The default implementation of [[ChangelogItemController]]
  * @param changelogItemModel The model for this controller.
  * @param artworkView The FXML view used to show cover artwork.
  * @param artistLabel The FXML label used to show the name of an artist.
  * @param albumLabel The FXML label used to show the name of an album.
  * @param updatedLabel The FXML label used to show when a change last occurred.
  * @param updatedPane The FXML pane used to contain the updated label.
  * @param progressPane The FXML pane used to contain the progress bar.
  * @param progressBar The FXML progress bar used to show the progress of synchronising this changelog item.
  */
class ChangelogItemControllerImpl(private val changelogItemModel: ChangelogItemModel,
                                  private val artworkView: ImageView,
                                  private val artistLabel: Label,
                                  private val albumLabel: Label,
                                  private val updatedLabel: Label,
                                  private val updatedPane: Pane,
                                  private val progressPane: Pane,
                                  private val progressBar: ProgressBar) extends ChangelogItemController {

  artworkView.image = Artwork(changelogItemModel.maybeArtwork)
  updatedLabel.text = changelogItemModel.at.format("HH:mm, EEE dd/MM/YYYY")

  changelogItemModel.maybeTags match {
    case Some(tags) =>
      artistLabel.text = tags.albumArtist
      albumLabel.text = tags.formattedAlbum
    case None =>
      albumLabel.text = changelogItemModel.albumRelativePath.toString
  }

  changelogItemModel.progress.onAltered {
    case Some((workDone, totalWork)) =>
      updatedPane.visible = false
      progressPane.visible = true
      progressBar.progress = workDone.toDouble / totalWork
    case _ =>
      updatedPane.visible = true
      progressPane.visible = false
  }
}
