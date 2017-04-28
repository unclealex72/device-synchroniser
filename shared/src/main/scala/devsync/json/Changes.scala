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

/**
  * Created by alex on 19/03/17
  **/
case class Changes(changes: Seq[Change])

sealed trait Change extends HasRelativePath {
  /**
    * The relative path of the file that changed.
    */
  val relativePath: RelativePath
  /**
    * The time the change occurred.
    */
  val at: IsoDate
}

case class Addition(relativePath: RelativePath, at: IsoDate, links: Links) extends Change with HasLinks


case class Removal(relativePath: RelativePath, at: IsoDate) extends Change
