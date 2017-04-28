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

import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{Files, Path}
import java.util.function.BiPredicate

import cats.data.EitherT
import devsync.json.DeviceDescriptor
import devsync.scalafx.util.PathResource._
import devsync.sync.Device

import scala.collection.JavaConversions._
import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by alex on 09/04/17
  * The default device discoverer that looks for a device descriptor in a directory, searching at most n levels down
  **/
class DeviceDiscovererImpl(val device: Device[Path]) extends DeviceDiscoverer {

  override def discover(root: Path, levels: Int)
                       (implicit ec: ExecutionContext): EitherT[Future, Exception, (DeviceDescriptor, Path)] = EitherT {
    Future {
      val directoryPredicate = new BiPredicate[Path, BasicFileAttributes] {
        override def test(p: Path, bfa: BasicFileAttributes): Boolean = bfa.isDirectory
      }
      val possibleRoots = Files.find(root, levels, directoryPredicate).iterator().toSeq
      device.findDeviceDescriptor(possibleRoots)
    }
  }
}