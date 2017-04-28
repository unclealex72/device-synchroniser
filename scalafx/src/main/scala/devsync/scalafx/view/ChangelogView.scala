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
  val changelogItemModelOrdering: Ordering[Node with ChangelogItemView] = Ordering.by(-_.model.value.at.date.getTime)

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