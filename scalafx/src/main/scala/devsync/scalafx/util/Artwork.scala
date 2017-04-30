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

import java.io.ByteArrayInputStream

import scalafx.scene.image.Image

/**
  * Generate artwork from a source.
  **/
object Artwork {

  /**
    * Generate artwork from a raw byte array or use a default image.
    * @param maybeData The artwork data, if any.
    * @return An image that contains either the supplied artwork data or a default image.
    */
  def apply(maybeData: Option[Array[Byte]]): Image = {
    maybeData match {
      case Some(data) => new Image(new ByteArrayInputStream(data))
      case _ => new Image("/no_artwork.png")
    }
  }

  /**
    * Get the default image.
    * @return The default image.
    */
  def empty: Image = apply(None)
}
