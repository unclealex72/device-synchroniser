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

package devsync.scalafx.model

import devsync.json.RelativePath

/**
  * Created by alex on 14/04/17
  **/
sealed trait SynchroniseAction

case class SynchroniseAddition(
                                maybeArtwork: Option[Array[Byte]],
                                artist: String,
                                album: String,
                                track: String) extends SynchroniseAction

case class SynchroniseRemoval(relativePath: RelativePath) extends SynchroniseAction

