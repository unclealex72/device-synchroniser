package devsync.scalafx

import javafx.collections.transformation.SortedList

import devsync.json.IsoDate

import scalafx.beans.property.ReadOnlyObjectProperty
import scalafx.collections.ObservableBuffer
import scalafx.scene.control.{Label, TableCell, TableColumn, TableView}
import scalafx.scene.image.ImageView
import scalafx.scene.layout.VBox
import devsync.scalafx.ObservableValues._

/**
  * An object to encapsulate the creation of the changelog table.
  *  Created by alex on 11/04/17
  **/
object ChangelogTable {

  def apply(items: ObservableBuffer[ChangelogRow]): TableView[ChangelogRow] = {
    val descriptionColumn = new TableColumn[ChangelogRow, Seq[String]] {
      cellValueFactory = { cdf =>
        new ReadOnlyObjectProperty[Seq[String]](this, "description", cdf.value.description)
      }
      sortable = false
      cellFactory = { _ =>
        new TableCell[ChangelogRow, Seq[String]] {
          item.onAltered { description =>
            val vbox = new VBox {
              children = description.map(line => new Label { text = line })
            }
            graphic = vbox
          }
        }
      }
      prefWidth = 180
    }

    val imageColumn = new TableColumn[ChangelogRow, Option[Array[Byte]]] {
      cellValueFactory = { cdf =>
        new ReadOnlyObjectProperty[Option[Array[Byte]]](this, "artwork", cdf.value.artwork)
      }
      sortable = false
      cellFactory = { _ =>
        val imageView = new ImageView {
          fitWidth = 50
          fitHeight = 50
        }
        new TableCell[ChangelogRow, Option[Array[Byte]]] {
          graphic = imageView
          item.onChange { (_, _, maybeData) =>
            imageView.image = Artwork(Option(maybeData).flatten)
          }
        }
      }
      prefWidth = 180
    }

    val tableView = new TableView[ChangelogRow] {
      columns ++= List(imageColumn, descriptionColumn)
    }
    tableView.delegate.setItems(
      new SortedList[ChangelogRow](
        items,
        Ordering.by(i => -i.modificationDate.date.getTime)))
    tableView
  }
}

case class ChangelogRow(artwork: Option[Array[Byte]], description: Seq[String], modificationDate: IsoDate)