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

import scalafx.beans.value.ObservableValue
import scalafx.event.subscriptions.Subscription

/**
  * Implicits for observable values
  **/
object ObservableValues {

  /**
    * Implicits for observable values
    * @param ov The observable value to enrich.
    * @tparam T The Scala type that will be returned for this Observable.
    * @tparam J The Java type to be wrapped by T.
    */
  implicit class ObservableValueImplicits[T, J](ov: ObservableValue[T, J]) {

    /**
      * A null safe version of onChange that responds to a value change if it is not null.
      * @param op The operation to run.
      * @return A subscription that will be run when the value changes.
      */
    def onAltered(op: J => Unit): Subscription = onAlteredOption { maybeValue =>
      maybeValue.foreach(op)
    }

    /**
      * A null safe version of onChange that responds to a value change and wraps it in an optional.
      * @param op The operation to run.
      * @return A subscription that will be run when the value changes.
      */
    def onAlteredOption(op: Option[J] => Unit): Subscription = ov.onChange { (_, oldValue, newValue) =>
      val maybeOldValue = Option(oldValue)
      val maybeNewValue = Option(newValue)
      if (maybeNewValue != maybeOldValue) op(maybeNewValue)
    }
  }
}
