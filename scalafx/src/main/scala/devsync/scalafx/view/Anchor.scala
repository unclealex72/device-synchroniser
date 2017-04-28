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

import scalafx.scene.Node
import scalafx.scene.layout.AnchorPane

/**
 * Helpers for anchor panes
 * Created by alex on 15/05/15.
 */
object Anchor {

  def newAnchor(
              node: Node,
              left: Option[Double] = None,
              right: Option[Double] = None,
              top: Option[Double] = None,
              bottom : Option[Double] = None): AnchorPane = new AnchorPane {
    children = Seq(anchor(node))
  }

  def anchor[N <: Node](
              node: N,
              left: Option[Double] = None,
              right: Option[Double] = None,
              top: Option[Double] = None,
              bottom : Option[Double] = None): N = {
    left.foreach(AnchorPane.setLeftAnchor(node, _))
    right.foreach(AnchorPane.setRightAnchor(node, _))
    top.foreach(AnchorPane.setTopAnchor(node, _))
    bottom.foreach(AnchorPane.setBottomAnchor(node, _))
    node
  }

  def newHorizontal(node: Node): AnchorPane = newAnchor(node = node, left = Some(0), right = Some(0))
  def newRight(node: Node): AnchorPane = newAnchor(node = node, right = Some(0))
  def newFill(node: Node): AnchorPane = newAnchor(node = node, left = Some(0), right = Some(0), top = Some(0), bottom = Some(0))
  def fill[N <: Node](node: N): N = anchor(node = node, left = Some(0), right = Some(0), top = Some(0), bottom = Some(0))
}
