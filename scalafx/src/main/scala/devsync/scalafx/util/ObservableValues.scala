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

package devsync.scalafx.util

import scalafx.beans.value.ObservableValue
import scalafx.event.subscriptions.Subscription

/**
  * Implicits for observable values
  *  Created by alex on 12/04/17
  **/
object ObservableValues {

  implicit class ObservableValueImplicits[T, J](ov: ObservableValue[T, J]) {

    /**
      * A null safe version of onChange that only presents the new value.
      * @param op
      * @tparam J
      * @return
      */
    def onAltered(op: J => Unit): Subscription = ov.onChange { (_, oldValue, newValue) =>
      if (newValue != null && newValue != oldValue) op(newValue)
    }
  }
}
