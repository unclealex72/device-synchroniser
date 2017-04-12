package devsync.scalafx

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
      * @tparam J1
      * @return
      */
    def onAltered[J1 >: J](op: J1 => Unit): Subscription = ov.onChange { (_, oldValue, newValue) =>
      if (newValue != null && newValue != oldValue) op(newValue)
    }
  }
}
