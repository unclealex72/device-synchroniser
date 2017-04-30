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

package devsync.logging

import devsync.json.IsoDate

/**
  * Contains objects that contain messages that can be displayed to a user.
  **/
object Messages {

  /**
    * Messages to do with finding a device.
    */
  object Discovery {
    val findDevice: String = "Find Device"
  }

  /**
    * Messages used to show progress during synchronisation.
    */
  object Sync {
    val notificationTitle: String = "Synchronising"
    def finished(count: Int): String = s"Successfully synchronised $count tracks."
    def failed(e: Exception, maybeIdx: Option[Int]): String =
      s"Synchronisation failed${maybeIdx.map(idx => s" for track $idx")}: ${e.getMessage}"
    val adding: String = "Adding"
    val removing: String = "Removing"
  }

  /**
    * Messages used to show information about [[Changes]]
    */
  object Changes {
    def maybeLastUpdated(maybeDate: Option[IsoDate]): String = maybeDate match {
      case Some(date) => s"Last updated at ${date.format("HH:mm:ss, EEE d MMM, yyyy")}"
      case _ => "Never updated"
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
