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

package devsync.sync

import scala.concurrent.Future
import devsync.json.{Addition, DeviceDescriptor, Removal, Tags}
import devsync.remote.ChangesClient

import scala.concurrent.ExecutionContext

/**
  * Created by alex on 03/04/17
  **/
trait Device[R] {

  def synchronise(root: R, changesClient: ChangesClient, deviceListener: DeviceListener[R])
                 (implicit resource: Resource[R],
                  resourceStreamProvider: ResourceStreamProvider[R],
                  ec: ExecutionContext): Future[Either[(Exception, Option[Int]), Int]]

  def findDeviceDescriptor(roots: Iterable[R])
                          (implicit resource: Resource[R],
                           resourceStreamProvider: ResourceStreamProvider[R]): Either[Exception, (DeviceDescriptor, R)]
}

trait DeviceListener[R] {
  def synchronisingStarting(): Unit

  def addingMusic(addition: Addition, maybeTags: Option[Tags], maybeArtwork: Option[Array[Byte]], number: Int, total: Int): Unit

  def musicAdded(addition: Addition, maybeTags: Option[Tags], maybeArtwork: Option[Array[Byte]], number: Int, total: Int, resource: R): Unit

  def musicRemoved(removal: Removal, number: Int, total: Int): Unit

  def removingMusic(removal: Removal, number: Int, total: Int): Unit

  def synchronisingFailed(e: Exception, maybeIdx: Option[Int]): Unit

  def synchronisingFinished(count: Int): Unit

}