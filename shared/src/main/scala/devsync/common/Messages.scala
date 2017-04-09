package devsync.common

import devsync.json.IsoDate

/**
  * Created by alex on 09/04/17
  **/
object Messages {

  object Discovery {
    val findDevice: String = "Find Device"
  }

  object Sync {
    val notificationTitle: String = "Synchronising"
    def finished(count: Int): String = s"Successfully synchronised $count tracks."
    def failed(e: Exception, maybeIdx: Option[Int]): String =
      s"Synchronisation failed${maybeIdx.map(idx => s" for track $idx")}: ${e.getMessage}"
    val adding: String = "Adding"
    val removing: String = "Removing"
  }

  object Changes {
    def maybeLastUpdated(maybeDate: Option[IsoDate]): String = maybeDate match {
      case Some(date) => s"Last updated at ${date.format("HH:mm:ss, EEE d MMM, yyyy")}"
      case None => "Never updated"
    }
    def changes(count: Int): String = count match {
      case 0 => "There are no changes"
      case 1 => "There is 1 change"
      case _ => s"There are $count changes"
    }
    val removed: String = "Removed"
    val synchronisationStarted: String = "Synchronisation started"
  }
}
