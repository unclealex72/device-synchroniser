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
  * A device represents a user owned device that can be synchronised.
  * @tparam R The type of files a device contains. There will need to be typeclasses for both `Resource[R]` and
  *           `ResourceStreamProvider[R]`. This then allows the Android filesystem and the Linux filesystem to
  *           be treated as one.
  **/
trait Device[R] {

  /**
    * Synchronise a device.
    * @param root The root file of the device.
    * @param changesClient The [[ChangesClient]] used to get changes from the Flac Manager server.
    * @param deviceListener A [[DeviceListener]] used to report on progress and errors.
    * @param resource A typeclass with file-like properties.
    * @param resourceStreamProvider A typeclass used to get a stream of data from a resource.
    * @param ec An execution context for running synchronisation in another thread.
    * @return Either the number of changes synchronised or an exception with an optional index if a specific
    *         change failed. This then allows a subsequent synchronisation to continue where this a failed one
    *         left off.
    */
  def synchronise(
                   root: R,
                   changesClient: ChangesClient,
                   deviceListener: DeviceListener[R])(implicit resource: Resource[R],
                                                      resourceStreamProvider: ResourceStreamProvider[R],
                                                      ec: ExecutionContext): Future[Either[(Exception, Option[Int]), Int]]

  /**
    * Find a [[DeviceDescriptor]] for this device.
    * @param roots A list of root directories to search.
    * @return Either a [[DeviceDescriptor]] and its location or an exception if one could not be found.
    */
  def findDeviceDescriptor(roots: Iterable[R])
                          (implicit resource: Resource[R],
                           resourceStreamProvider: ResourceStreamProvider[R])
  : Either[Exception, (DeviceDescriptor, R)]
}

trait DeviceListener[R] {

  /**
    * Notify a user that synchronisation is about to start.
    */
  def synchronisingStarting(): Unit

  /**
    * Notify a user that a track is about to be added.
    * @param addition The [[Addition]] that is about to take place.
    * @param maybeTags The [[Tags]] for the addition if they could be loaded.
    * @param maybeArtwork The album artwork for the addition if they could be loaded.
    * @param number The index of this change.
    * @param total The total number of changes.
    */
  def addingMusic(
                   addition: Addition,
                   maybeTags: Option[Tags],
                   maybeArtwork: Option[Array[Byte]],
                   number: Int,
                   total: Int): Unit

  /**
    * Notify a user that a track has been added.
    * @param addition The [[Addition]] that has just taken place.
    * @param maybeTags The [[Tags]] for the addition if they could be loaded.
    * @param maybeArtwork The album artwork for the addition if they could be loaded.
    * @param number The index of this change.
    * @param total The total number of changes.
    */
  def musicAdded(
                  addition: Addition,
                  maybeTags: Option[Tags],
                  maybeArtwork: Option[Array[Byte]],
                  number: Int,
                  total: Int,
                  resource: R): Unit

  /**
    * Notify a user that a track is about to be removed.
    * @param removal The [[Removal]] that is about to take place.
    * @param number The index of this change.
    * @param total The total number of changes.
    */
  def removingMusic(removal: Removal, number: Int, total: Int): Unit

  /**
    * Notify a user that a track has been removed.
    * @param removal The [[Removal]] that has taken place.
    * @param number The index of this change.
    * @param total The total number of changes.
    */
  def musicRemoved(removal: Removal, number: Int, total: Int): Unit

  /**
    * Notify a user that synchronising has failed.
    * @param e The exception that caused the failure.
    * @param maybeIdx If the failure occurred whilst processing a specific change then this will be its index.
    *                 Otherwise, none.
    */
  def synchronisingFailed(e: Exception, maybeIdx: Option[Int]): Unit

  /**
    * Notify a user that synchronisation was successful.
    * @param count The number of tracks that were changed.
    */
  def synchronisingFinished(count: Int): Unit

}