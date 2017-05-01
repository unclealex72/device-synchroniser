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

import android.view.View

/**
  * An abstract base class used to create extractors for different UI components. These destructors can then
  * be used in [[https://github.com/47deg/macroid Macroid's]] `Transform` class.
  *
  * @param id The ID of the component.
  * @tparam V The type of the component.
  */
abstract class ViewExtractor[V <: View](val id: Int) {
  def unapply(view: View): Option[V] = view match {
    case v: V @unchecked if v.getId == id => Some(v)
    case _ => None
  }
}

