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

import javafx.fxml.FXMLLoader
import javafx.scene.{Node => JNode, Parent => JParent}

import scalafx.Includes._
import scalafx.scene.Parent

/**
  * An object used to load .fxml views and to extract components from them.
  */
object View {

  def apply(viewName: String): Parent = {
    val resourceName = s"/views/$viewName.fxml"
    val resource = getClass.getResource(resourceName)
    FXMLLoader.load[JParent](resource)
  }

  /**
    * A small method used to build [[ViewAndController]]s. This class is parameterised to allow the
    * implementation type of the controller to be hidden.
    * @param parent The parent node to use as a view.
    * @tparam I The public type of the controller that the resulting [[ViewAndController]] will contain.
    */
  private[View] class ViewAndControllerBuilder[I](parent: Parent) {
    def apply[C <: I](controller: C) = new ViewAndController[I](parent, controller)
  }

  /**
    * Enhance parent nodes so that typed child nodes can be found.
    * @param parent The parent node to enhance.
    */
  implicit class ParentImplicits(parent: Parent) {

    /**
      * Look up a component.
      * @param name The name of the component.
      * @tparam N The type of the component.
      * @return The component with the given name.
      */
    def component[N <: JNode](name: String): N = parent.delegate.lookup(s"#$name").asInstanceOf[N]

    /**
      * Create a new [[ViewAndControllerBuilder]]
      * @tparam I The public type of the controller that the [[ViewAndController]] will contain.
      * @return A new [[ViewAndControllerBuilder]] containing the parent node as its view.
      */
    def withController[I]: ViewAndControllerBuilder[I] = new ViewAndControllerBuilder[I](parent)
  }
}
