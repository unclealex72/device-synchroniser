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
  * Created by alex on 14/04/17
  **/
trait SynchronisingView extends Dimensions {

  val dimensions: Dimension2D = new Dimension2D(width = 800, height = 155)

  def action: ObjectProperty[Option[SynchroniseAction]]

  def action_=(sa: Option[SynchroniseAction]): Unit = {
    action() = sa
  }

  def workDone: LongProperty

  def workDone_=(_workDone: Long): Unit = {
    workDone() = _workDone
  }

  def totalWork: LongProperty

  def totalWork_=(_totalWork: Long): Unit = {
    totalWork() = _totalWork

  }
}

object SynchronisingView {

  private val defaultHeight: Double = 128
  private val defaultWidth: Double = 600

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