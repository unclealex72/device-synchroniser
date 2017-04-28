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

package devsync.scalafx.sync

import java.nio.file.Path

import cats.data.EitherT
import devsync.json.DeviceDescriptor

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by alex on 09/04/17
  *
  * A trait used to find devices somewhere arbitrarily on file system
  **/
trait DeviceDiscoverer {

  def discover(root: Path, levels: Int)(implicit ec: ExecutionContext): EitherT[Future, Exception, (DeviceDescriptor, Path)]
}
