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

import javafx.collections.ObservableList
import javafx.scene.{control => jfxc}
import javafx.{scene => jfx}

import com.sun.javafx.collections.ImmutableObservableList
import devsync.scalafx.ObservableValues._
import devsync.scalafx.View._

import scalafx.Includes._
import scalafx.scene.Node
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.control._
import scalafx.stage.{Modality, Stage}

/**
  * The main controller for showing a list of changes.
  */
trait ChangelogController {

  /**
    * Indicate that the application is searching for the Flac Manager server and a device descriptor.
    */
  def searching(): Unit

  /**
    * Indicate to a user that there are no changes to process.
    */
  def noChanges(): Unit

  /**
    * Indicate to a user that there are changes to process.
    */
  def changes(): Unit

  /**
    * Indicate to a user that the application is synchronising.
    */
  def synchronising(): Unit

  /**
    * Indicate that synchronising has finished.
    * @param count The number of tracks processed.
    */
  def finished(count: Int): Unit

  /**
    * Indicate that synchronising failed.
    * @param ex The exception that caused the failure.
    */
  def error(ex: Exception): Unit

  /**
    * The changelog item models that this controller can display.
    * @return An observable list of changes that can be mutated to show what changes need to be made as well
    *         as the progress of synchronisation.
    */
  def changelogItems(): ObservableList[ChangelogItemModel]

  /**
    * Present the user with a one line message.
    * @param topLine The top line message.
    */
  def messages(topLine: String): Unit

  /**
    * Present the user with a two line message.
    * @param topLine The top line message.
    * @param bottomLine The bottom line message.
    */
  def messages(topLine: String, bottomLine: String): Unit

  /**
    * Set the action to be executed when the synchronise button is pressed.
    * @param callback The code to execute when the synchronise button is pressed.
    */
  def synchronise(callback: => Unit): Unit
}

/**
  * Used to create instances of [[ChangelogController]].
  */
object ChangelogController {

  /**
    * Create a new [[ChangelogController]]
    * @param stage The main JavaFX stage, required for showing alerts.
    * @param shutdownCallback A callback used to shutdown the application.
    * @return A new [[ChangelogController]] and it's associated FXML view.
    */
  def apply(stage: Stage)(shutdownCallback: => Unit): ViewAndController[ChangelogController] = {
    val view = View("changelog")
    val searchingPane = view.component[jfx.Node]("searchingPane")
    val changesPane = view.component[jfx.Node]("changesPane")
    val syncButton = view.component[jfxc.Button]("syncButton")
    val listView = view.component[jfxc.ListView[ChangelogItemModel]]("listView")
    val topLabel = view.component[jfxc.Label]("topLabel")
    val bottomLabel = view.component[jfxc.Label]("bottomLabel")

    view.withController[ChangelogController](
      new ChangelogControllerImpl(
        stage, () => shutdownCallback,
        searchingPane, changesPane, syncButton,
        listView, topLabel, bottomLabel))
  }
}

/**
  * The default implementation of [[ChangelogController]]
  * @param stage The stage for the application.
  * @param shutdownCallback A callback used to shut down the application.
  * @param searchingPane The pane used to show the user that the application is searching for the Flac Manager server
  *                      and for a device.
  * @param changesPane The pane used to show a list of changelog items to a user.
  * @param syncButton The synchronise button.
  * @param listView The list view used to show a list of changelog items to a user.
  * @param topLabel The top level label.
  * @param bottomLabel The bottom level label.
  */
class ChangelogControllerImpl(
                             private val stage: Stage,
                             private val shutdownCallback: () => Unit,
                             private val searchingPane: Node,
                             private val changesPane: Node,
                             private val syncButton: Button,
                             private val listView: ListView[ChangelogItemModel],
                             private val topLabel: Label,
                             private val bottomLabel: Label
                             ) extends ChangelogController {

  /**
    * Disable selecting items in the changelog list.
    */
  listView.selectionModel = new jfxc.MultipleSelectionModel[ChangelogItemModel] {
    val emptyItems = new ImmutableObservableList[ChangelogItemModel]()
    val emptyIndicies = new ImmutableObservableList[Integer]()

    override def getSelectedItems: ObservableList[ChangelogItemModel] = emptyItems
    override def getSelectedIndices: ObservableList[Integer] = emptyIndicies
    override def selectFirst(): Unit = {}
    override def selectAll(): Unit = {}
    override def selectLast(): Unit = {}
    override def selectIndices(index: Int, indices: Int*): Unit = {}
    override def select(index: Int): Unit = {}
    override def select(obj: ChangelogItemModel): Unit = {}
    override def selectPrevious(): Unit = {}
    override def selectNext(): Unit = {}
    override def clearAndSelect(index: Int): Unit = {}
    override def clearSelection(index: Int): Unit = {}
    override def clearSelection(): Unit = {}
    override def isSelected(index: Int): Boolean = false
    override def isEmpty: Boolean = false
  }

  /**
    * A [[ListCell]] that uses a [[ChangelogItemController]]'s view to display a changelog item.
    */
  class ChangelogItemCell extends ListCell[ChangelogItemModel]() {
    item.onAlteredOption { maybeChangelogItemModel =>
      graphic = maybeChangelogItemModel.map(ChangelogItemController(_).view).orNull
    }
  }

  listView.cellFactory = _ => new ChangelogItemCell()
  listView.items()

  /**
    * Hide all panes.
    */
  def hideAll(): Unit = {
    searchingPane.visible = false
    changesPane.visible = false
    syncButton.disable = true
  }

  /**
    * @inheritdoc
    */
  override def synchronising(): Unit = {
    syncButton.disable = true
  }

  /**
    * @inheritdoc
    */
  override def searching(): Unit = {
    hideAll()
    searchingPane.visible = true
  }

  /**
    * @inheritdoc
    */
  override def noChanges(): Unit = {
    buildAlert(AlertType.Information, "This device is up to date.", "")
  }

  /**
    * @inheritdoc
    */
  override def changes(): Unit = {
    hideAll()
    changesPane.visible = true
    syncButton.disable = false
  }

  /**
    * Build an alert dialogue that will exit the application once the "OK" button is clicked.
    * @param alertType The type of alert.
    * @param header The alert's header text.
    * @param content The alert's content text.
    */
  def buildAlert(alertType: AlertType, header: String, content: String): Unit = {
    new Alert(alertType) {
      initOwner(stage)
      title = "Device Synchroniser+"
      headerText = header
      contentText = content
      initModality(Modality.ApplicationModal)
      resizable = true
    }.showAndWait()
    shutdownCallback()
  }

  /**
    * @inheritdoc
    */
  override def finished(count: Int): Unit = {
    changelogItems().clear()
    messages("Completed")
    buildAlert(AlertType.Information, "Synchronising was successful", s"Successfully synchronised $count tracks")
  }


  /**
    * @inheritdoc
    */
  override def error(ex: Exception): Unit = {
    messages("Failed")
    buildAlert(AlertType.Error, "An unexpected error occurred", ex.getMessage)
  }

  /**
    * @inheritdoc
    */
  override def messages(topLine: String): Unit = {
    showMessages(Some(topLine), None)
  }

  /**
    * Show up to two lines of a message.
    * @param maybeTopLine The first line of a message, if any.
    * @param maybeBottomLine The second line of a message, if any.
    */
  def showMessages(maybeTopLine: Option[String], maybeBottomLine: Option[String]): Unit = {
    Map(topLabel -> maybeTopLine, bottomLabel -> maybeBottomLine).foreach {
      case (label, maybeText) => label.text = maybeText.getOrElse("")
    }
  }

  /**
    * @inheritdoc
    */
  override def messages(topLine: String, bottomLine: String): Unit = {
    showMessages(Some(topLine), Some(bottomLine))
  }

  /**
    * @inheritdoc
    */
  override def synchronise(callback: => Unit): Unit = {
    syncButton.onAction = handle(callback)
  }

  /**
    * @inheritdoc
    */
  override def changelogItems(): ObservableList[ChangelogItemModel] = {
    listView.items.value
  }
}