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
import scalafx.geometry.Insets
/**
  * Created by alex on 14/04/17
  **/
trait DeviceInformationView {
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
    items.onChange {
      val count = items.size
      synchroniseButton.disable = count == 0
      synchroniseButton.text = if (count == 0) "No Changes" else s"Synchronise $count change${if (count != 1) "s" else ""}"
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
    }
  }
}