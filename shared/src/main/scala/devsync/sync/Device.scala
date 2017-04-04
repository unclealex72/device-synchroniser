package devsync.sync

import scala.concurrent.Future
import devsync.json.{Addition, DeviceDescriptor, Removal, Tags}
import devsync.remote.ChangesClient

import scala.concurrent.ExecutionContext

/**
  * Created by alex on 03/04/17
  **/
trait Device[R] {

  def synchronise(root: R, changesClient: ChangesClient, deviceListener: DeviceListener[R], deviceDescriptor: DeviceDescriptor)
                 (implicit resource: Resource[R],
                  resourceStreamProvider: ResourceStreamProvider[R],
                  ec: ExecutionContext): Future[Either[(Exception, Option[Int]), Int]]

  def findDeviceDescriptor(roots: Iterable[R])
                          (implicit resource: Resource[R],
                           resourceStreamProvider: ResourceStreamProvider[R]): Either[Exception, DeviceDescriptor]
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