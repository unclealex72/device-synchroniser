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

/**
  * Constants that are used in various places.
  **/
object Constants {

  /**
    * Constants for intents.
    */
  object Intent {

    /**
      * The intent extras key for the location of a user's local music directory.
      */
    val resourceUri = "resourceUri"

    /**
      * The intent extras key for the content of a user's device descriptor file.
      */
    val deviceDescriptor = "deviceDescriptor"

    /**
      * The URL of a Flac Manager server.
      */
    val serverUrl = "serverUrl"
  }

  /**
    * Constants for preferences.
    */
  object Prefs {

    /**
      * The preference key used to store the location of a user's local music directory.
      */
    val resourceUri = "resourceUri"
  }
}
