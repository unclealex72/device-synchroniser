package devsync.scalafx

import javafx.collections.transformation.SortedList

import devsync.json.IsoDate

import scalafx.beans.property.ReadOnlyObjectProperty
import scalafx.collections.ObservableBuffer
import scalafx.scene.control.{Label, TableCell, TableColumn, TableView}
import scalafx.scene.image.ImageView
import scalafx.scene.layout.VBox

/**
  * An object to encapsulate the creation of the changelog table.
  *  Created by alex on 11/04/17
  **/
object ChangelogTable {

  def apply(items: ObservableBuffer[ReadOnlyChangelogItem]): TableView[ReadOnlyChangelogItem] = {
    val descriptionColumn = new TableColumn[ReadOnlyChangelogItem, Seq[String]] {
      cellValueFactory = {
        _.value.description
      }
      sortable = false
      cellFactory = { _ =>
        new TableCell[ReadOnlyChangelogItem, Seq[String]] {
          item.onChange { (_, _, description) =>
            val vbox = new VBox {
              children = Option(description).getOrElse(Seq.empty).map(line => new Label { text = line })
            }
            graphic = vbox
          }
        }
      }
      prefWidth = 180
    }

    val imageColumn = new TableColumn[ReadOnlyChangelogItem, Option[Array[Byte]]] {
      cellValueFactory = {
        _.value.artwork
      }
      sortable = false
      cellFactory = { _ =>
        val imageView = new ImageView {
          fitWidth = 50
          fitHeight = 50
        }
        new TableCell[ReadOnlyChangelogItem, Option[Array[Byte]]] {
          graphic = imageView
          item.onChange { (_, _, maybeData) =>
            imageView.image = Artwork(Option(maybeData).flatten)
          }
        }
      }
      prefWidth = 180
    }

    val tableView = new TableView[ReadOnlyChangelogItem] {
      columns ++= List(imageColumn, descriptionColumn)
    }
    tableView.delegate.setItems(
      new SortedList[ReadOnlyChangelogItem](
        items,
        Ordering.by(i => -i._modificationDate.date.getTime)))
    tableView
  }
}

case class ReadOnlyChangelogItem(_artwork: Option[Array[Byte]], _description: Seq[String], _modificationDate: IsoDate) {
  val artwork = new ReadOnlyObjectProperty[Option[Array[Byte]]](this, "artwork", _artwork)
  val description = new ReadOnlyObjectProperty[Seq[String]](this, "artwork", _description)
  val modificationDate = new ReadOnlyObjectProperty[IsoDate](this, "modificationDate", _modificationDate)
}