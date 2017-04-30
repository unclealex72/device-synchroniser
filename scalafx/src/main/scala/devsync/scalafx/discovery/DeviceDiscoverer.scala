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

package devsync.scalafx.discovery

import java.nio.file.Path

import cats.data.EitherT
import devsync.json.DeviceDescriptor

import scala.concurrent.{ExecutionContext, Future}

/**
  * A trait used to find devices somewhere arbitrarily on file system.
  **/
trait DeviceDiscoverer {

  /**
    * Look for a device starting at a given directory and search at most _n_ directories down.
    * @param root The root directory to start looking.
    * @param levels The maximum number of levels to search.
    * @param ec An execution context used to find the device asynchronously.
    * @return Eventually either a device descriptor and a path or an exception if a device cannot be found.
    */
  def discover(root: Path,
               levels: Int)
              (implicit ec: ExecutionContext): EitherT[Future, Exception, (DeviceDescriptor, Path)]
}
