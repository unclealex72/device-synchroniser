package devsync.scalafx

import scalafx.scene.Node
import scalafx.scene.layout.AnchorPane

/**
 * Helpers for anchor panes
 * Created by alex on 15/05/15.
 */
object Anchor {

  def right(node: Node): AnchorPane = new AnchorPane {
    children = node
    AnchorPane.setRightAnchor(node, 0)
  }

  def fill(node: Node): AnchorPane = new AnchorPane {
    children = node
    AnchorPane.setRightAnchor(node, 0)
    AnchorPane.setLeftAnchor(node, 0)
    AnchorPane.setTopAnchor(node, 0)
    AnchorPane.setBottomAnchor(node, 0)
  }
}
