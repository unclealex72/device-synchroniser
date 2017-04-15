package devsync.scalafx.view

import javafx.collections.transformation.SortedList

import com.typesafe.scalalogging.StrictLogging
import devsync.scalafx.model.ChangelogItemModel

import scalafx.collections.ObservableSet.{Add, Remove}
import scalafx.collections.{ObservableBuffer, ObservableSet}
import scalafx.scene.Node
import scalafx.scene.layout.VBox
import scalafx.scene.text.Font
import scala.collection.JavaConversions._


/**
  * Created by alex on 14/04/17
  **/
trait ChangelogView {

  def items: ObservableSet[ChangelogItemModel]
}

object ChangelogView extends StrictLogging {

  /**
    * Order children by newest first
    */
  val changelogItemModelOrdering: Ordering[Node with ChangelogItemView] = Ordering.by(_.model.value.at.date.getTime)

  def apply(models: ObservableSet[ChangelogItemModel])(implicit defaultFont: Font): Node with ChangelogView = {
    val children = ObservableBuffer.empty[Node with ChangelogItemView]

    models.onChange { (_, change) =>
      change match {
        case Add(newItem) =>
          children += ChangelogItemView(newItem)
        case Remove(oldItem) =>
          val maybeFoundItem = children.find(_.model == oldItem)
          children --= maybeFoundItem
      }
    }

    val result = new VBox with ChangelogView {
      spacing = 5.0
      def items: ObservableSet[ChangelogItemModel] = models
    }
    children.onChange {
      val sortedChildren = new SortedList[Node with ChangelogItemView](children, changelogItemModelOrdering)
      result.children = sortedChildren.iterator().toSeq
    }
    result
  }
}