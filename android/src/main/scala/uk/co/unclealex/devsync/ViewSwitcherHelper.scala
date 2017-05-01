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

package uk.co.unclealex.devsync

import android.widget.ViewSwitcher
import macroid.Ui

/**
  * Implicits to extra functionality to Android's `ViewSwitcher`.
  */
object ViewSwitcherHelper {

  /**
    * Implicits to extra functionality to Android's `ViewSwitcher`.
    * @param viewSwitcher The view switcher to enhance.
    */
  implicit class ViewSwitcherHelperImplicits(viewSwitcher: ViewSwitcher) {

    /**
      * Show the child with the given ID.
      * @param id The ID to search for.
      * @return A UI containing true if a child was found, false otherwise.
      */
    def showChildWithId(id: Int): Ui[Boolean] = Ui {
      val maybeChildIdx = Range(0, viewSwitcher.getChildCount).find(idx => viewSwitcher.getChildAt(idx).getId == id)
      maybeChildIdx match {
        case Some(idx) =>
          viewSwitcher.setDisplayedChild(idx)
          true
        case None => false
      }
    }
  }
}
