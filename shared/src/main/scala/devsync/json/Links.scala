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

package devsync.json

import java.net.URL

/**
  * Created by alex on 21/03/17
  **/
case class Links(
                  /**
                    * A link to the music data.
                    */
                  music: URL,
                  /**
                    * A link to the tags for the music file.
                    */
                  tags: URL,
                  /**
                    * A link to the artwork for the track.
                    */
                  artwork: URL) extends HasLinks {
  override val links: Links = this
}

/**
  * A trait for classes that have the links as above.
  */
trait HasLinks {
  val links: Links
}