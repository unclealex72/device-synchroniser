package devsync.sync

import devsync.json.Change

/**
  * Created by alex on 27/03/17
  **/
trait DeviceListener {
  def ongoing(idx: Int, changeCount: Int, change: Change) = Unit

  def completed(changeCount: Int) = Unit

  def failed(e: Exception) = Unit

}
