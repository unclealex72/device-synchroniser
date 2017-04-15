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
  * Created by alex on 14/04/17
  **/
trait DeviceInformationView {

  def ready: BooleanProperty
  def ready_=(b: Boolean): Unit = {
    ready() = b
  }
}

object DeviceInformationView {

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