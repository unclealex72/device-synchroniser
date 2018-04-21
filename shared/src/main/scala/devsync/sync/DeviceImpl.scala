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

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import cats.data.EitherT
import cats.syntax.either._
import cats.implicits._
import com.typesafe.scalalogging.StrictLogging
import devsync.json._
import devsync.remote.ChangesClient
import org.threeten.bp.Clock

import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source
import scala.util.{Failure, Success, Try}

/**
  * The default implementation of [[Device]]
  * @param jsonCodec The [[JsonCodec]] used to decode JSON objects from the Flac Manager server.
  * @param clock The [[Clock]] used to get the current time.
  * @param faultTolerance Fault tolerance patterns.
  * @tparam R The type of files a device contains. There will need to be typeclasses for both `Resource[R]` and
  *           `ResourceStreamProvider[R]`. This then allows the Android filesystem and the Linux filesystem to
  *           be treated as one.
  */
class DeviceImpl[R](jsonCodec: JsonCodec,
                    clock: Clock, faultTolerance: FaultTolerance) extends Device[R] with StrictLogging {

  /**
    * The name the a device descriptor filename.
    */
  val DESCRIPTOR_FILENAME = "device.json"

  /**
    * A case class that contains an exception and maybe an index of a failed change.
    * @param e An exception.
    * @param maybeIdx The index of the change that failed or none if the failure was not specific to one change.
    */
  case class ExceptionWithMaybeIndex(e: Exception, maybeIdx: Option[Int] = None)

  /**
    * A shorthand for [[ExceptionWithMaybeIndex]]
    */
  type EWMI = ExceptionWithMaybeIndex

  /**
    * @inheritdoc
    */
  override def synchronise(
                            root: R,
                            changesClient: ChangesClient,
                            deviceListener: DeviceListener[R])(implicit resource: Resource[R],
                                                               resourceStreamProvider: ResourceStreamProvider[R],
                                                               executionContext: ExecutionContext): Either[(Exception, Option[Int]), Int] = {
    deviceListener.synchronisingStarting()
    reloadDeviceDescriptor(root) match {
      case Success(deviceDescriptor) =>
        val synchronise: Either[EWMI, Int] = new Synchroniser(root, changesClient, deviceListener, deviceDescriptor).synchronise
        synchronise.leftMap(ewmi => (ewmi.e, ewmi.maybeIdx))
      case Failure(e : Exception) =>
        deviceListener.synchronisingFailed(e, None)
        Left(e, None)
    }
  }


  /**
    * A class that actually performs the synchronisation of a device with the Flac Manager server.
    * @param root The root of the device.
    * @param changesClient The [[ChangesClient]] used to download changes from the Flac Manager server.
    * @param deviceListener A [[DeviceListener]] used to provide feedback to a user.
    * @param deviceDescriptor The [[DeviceDescriptor]] file that describes the device.
    * @param resource A typeclass with file-like properties.
    * @param resourceStreamProvider A typeclass used to get a stream of data from a resource.
    */
  class Synchroniser(root: R, changesClient: ChangesClient, deviceListener: DeviceListener[R], deviceDescriptor: DeviceDescriptor)(implicit resource: Resource[R],
                                                                                                                                   resourceStreamProvider: ResourceStreamProvider[R]) {

    /**
      * Synchronise the device.
      * @return Eventually either an [[EWMI]] or the number of changes.
      */
    def synchronise: Either[EWMI, Int] = {
      val wrappedResult: Either[EWMI, Int] = for {
        changes <- loadChanges
        result <- processChanges(changes)
      } yield result
      updateDeviceDescriptor(deviceDescriptor, wrappedResult) match {
        case Success(_) => wrappedResult match {
          case Right(count) =>
            logger.info("Synchronising completed successfully.")
            deviceListener.synchronisingFinished(count)
            Right(count)
          case Left(ewmi) =>
            val (e, idx) = (ewmi.e, ewmi.maybeIdx)
            logger.error(s"Synchronising Failed at index $idx:", e)
            deviceListener.synchronisingFailed(e, idx)
            Left(ewmi)
        }
        case Failure(e: Exception) =>
          logger.error("Synchronising Failed:", e)
          deviceListener.synchronisingFailed(e, None)
          Left(ExceptionWithMaybeIndex(e, None))
      }

    }

    /**
      * Load the [[Changes]] from a Flac Manager server.
      * @return Eventually either a [[Changes]] object or a failure.
      */
    def loadChanges: Either[EWMI, Changes] = {
      changesClient.changesSince(deviceDescriptor.user, deviceDescriptor.extension, deviceDescriptor.maybeLastModified) match {
        case Success(changes) => Right(changes)
        case Failure(ex: Exception) => Left(ExceptionWithMaybeIndex(ex))
      }
    }

    /**
      * Process and synchronise a list of [[Change]]s.
      * @param changes The [[Changes]] object downloaded from the Flac Manager server.
      * @return Eventually either the number of changes or a failure.
      */
    def processChanges(changes: Changes): Either[EWMI, Int] = {
      val empty: Either[EWMI, Unit] = Right({})
      val total = changes.changes.size
      val richChangeWithProgressBuilder = RichChangeWithProgress(total)
      val previouslyUntriedChanges: Seq[(Change, Int)] =
        changes.changes.zipWithIndex.drop(deviceDescriptor.maybeOffset.getOrElse(0))
      previouslyUntriedChanges.foldLeft(empty) { (acc, changeWithIndex) =>
        acc.flatMap { _ =>
          val change: Change = changeWithIndex._1
          val idx: Int = changeWithIndex._2
          val richChange: RichChange = change match {
            case addition: Addition =>
              val maybeTags: Option[Tags] = changesClient.tags(addition).toOption
              val maybeArtwork: Option[Array[Byte]] = {
                val buff = new ByteArrayOutputStream()
                IO.closingTry(buff)(changesClient.artwork(addition, _)).toOption.map(_ => buff.toByteArray)
              }
              RichAddition(addition, maybeTags, maybeArtwork)
            case removal: Removal => RichRemoval(removal)
          }
          val richChangeWithProgress = richChangeWithProgressBuilder(richChange, idx)
          processRichChangeWithProgress(richChangeWithProgress) match {
            case Success(rcwp) => Right(rcwp)
            case Failure(ex: Exception) => Left(ExceptionWithMaybeIndex(ex, Some(idx)))
          }
        }
      }.map(_ => total)
    }

    /**
      * A rich change is a [[Change]] that may be decorated by supporting [[Tags]] and album artwork. If either or neither
      * cannot be downloaded this is not seen as an error.
      */
    sealed trait RichChange

    /**
      * A rich addition that may also contain [[Tags]] and album artwork.
      * @param addition The original [[Change]]
      * @param maybeTags [[Tags]] for the addition, if they could be loaded.
      * @param maybeArtwork Artwork for the addition, if it could be loaded.
      */
    case class RichAddition(addition: Addition, maybeTags: Option[Tags], maybeArtwork: Option[Array[Byte]]) extends RichChange

    /**
      * A rich removal that contains nothing but the original change.
      * @param removal The original [[Change]]
      */
    case class RichRemoval(removal: Removal) extends RichChange

    /**
      * A case class that extends a [[RichChange]] with the index of the change and the total number of changes. This
      * is used to report progress in the [[DeviceListener]].
      *
      * @param richChange The original rich change.
      * @param progress The progress of for the whole synchronisation.
      */
    case class RichChangeWithProgress(richChange: RichChange, progress: Progress)

    /**
      * An object used to create [[RichChangeWithProgress]]es.
      */
    object RichChangeWithProgress {

      /**
        * A function that creates a new [[RichChangeWithProgress]] from an index and a [[RichChange]].
        * @param total The total number of changes.
        * @return A function that creates a new [[RichChangeWithProgress]] from an index and a [[RichChange]].
        */
      def apply(total: Int): (RichChange, Int) => RichChangeWithProgress = {
        case (change, number) =>
          RichChangeWithProgress(change, Progress(number, total))
      }
    }

    /**
      * Process a [[RichChangeWithProgress]] by either adding or removing the track from the device and notifying a
      * user through the [[DeviceListener]]
      * @param richChangeWithProgress The change to process.
      * @return Eventually [[Unit]] or an exception.
      */
    def processRichChangeWithProgress(richChangeWithProgress: RichChangeWithProgress): Try[Unit] = {
      // When adding and removing music we need to make sure that the addingMusic and removingMusic events are
      // always called before musicAdded and musicRemoved.
      richChangeWithProgress.richChange match {
        case RichAddition(addition, maybeTags, maybeArtwork) =>
          deviceListener.addingMusic(addition, maybeTags, maybeArtwork, richChangeWithProgress.progress)
          addMusic(addition).map { file =>
            deviceListener.musicAdded(addition, maybeTags, maybeArtwork, richChangeWithProgress.progress, file)
          }
        case RichRemoval(removal) =>
          deviceListener.removingMusic(removal, richChangeWithProgress.progress)
          removeMusic(removal).map { _ =>
            deviceListener.musicRemoved(removal, richChangeWithProgress.progress)
          }
      }
    }

    /**
      * Add a track to the device.
      * @param addition The track to add.
      * @return Eventually either the resource that was newly created or an exception.
      */
    def addMusic(addition: Addition): Try[R] = {
      addition.relativePath match {
        case rp @ DirectoryAndFile(dir, name) =>
          logger.info(s"Adding $rp")
          faultTolerance.tolerate {
            for {
              directory <- resource.mkdirs(root, dir)
              file <- resource.findOrCreateResource(directory, "audio/mp3", name)
              _ <- resource.writeTo(file, out => changesClient.music(addition, out))
            } yield {
              file
            }
          }
        case _ =>
          Try(throw new IllegalArgumentException(s"Relative path ${addition.relativePath} does not point to a file and directory."))
      }
    }

    /**
      * Remove a track from the device.
      * @param removal The track to remove.
      * @return Eventuall either [[Unit]] or an exception.
      */
    def removeMusic(removal: Removal): Try[Unit] = {
      val path: RelativePath = removal.relativePath
      logger.info(s"Removing $path")
      Try(resource.find(root, path).foreach(resource.removeAndCleanDirectories))
    }

    /**
      * Update the device descriptor with the result of this synchronisation.
      * @param deviceDescriptor The device descriptor to update.
      * @param accumulatedResult The number of changes that were synchronised or an exception with maybe an index.
      * @return Either [[Unit]] or an exception if the device descriptor could not be saved back to the device.
      */
    def updateDeviceDescriptor(deviceDescriptor: DeviceDescriptor, accumulatedResult: Either[EWMI, Int]): Try[Unit] = {
      // Finished - log whether synchronisation was successful or not and write the new device descriptor back to the
      val newDeviceDescriptor: DeviceDescriptor = accumulatedResult match {
        case Right(_) =>
          deviceDescriptor.copy(maybeLastModified = Some(clock.instant()), maybeOffset = None)
        case Left(ewmi) =>
          // If the failed index is empty, keep the old index.
          logger.error(s"Synchronising failed at index ${ewmi.maybeIdx}", ewmi.e)
          deviceDescriptor.copy(maybeOffset = ewmi.maybeIdx.orElse(deviceDescriptor.maybeOffset))
      }
      for {
        deviceDescriptorFile <- resource.findOrCreateResource(root, "application/json", DESCRIPTOR_FILENAME)
        _ <- resource.writeTo(deviceDescriptorFile, out => {
          IO.closing(new ByteArrayInputStream(jsonCodec.writeDeviceDescriptor(newDeviceDescriptor).getBytes("UTF-8"))) { in =>
            IO.copy(in, out)
          }
        })
      } yield {}
    }
  }

  /**
    * @inheritdoc
    */
  override def findDeviceDescriptor(roots: Iterable[R])
                                   (implicit resource: Resource[R],
                                    resourceStreamProvider: ResourceStreamProvider[R]): Try[(DeviceDescriptor, R)] = {
    def findDeviceDescriptorResource(root: R): Try[R] = {
      resource.find(root, RelativePath(DESCRIPTOR_FILENAME)) match {
        case Some(deviceDescriptorResource) => Success(deviceDescriptorResource)
        case _ => Try(throw new IllegalStateException(s"Cannot find a device descriptor resource for $root"))
      }
    }

    def canWriteDeviceDescriptorResource(deviceDescriptorResource: R): Try[R] = {
      if (resource.canWrite(deviceDescriptorResource)) {
        Success(deviceDescriptorResource)
      }
      else {
        Try(throw new IllegalStateException(s"$deviceDescriptorResource is not writeable"))
      }
    }

    val empty: Try[(DeviceDescriptor, R)] = Try(throw new RuntimeException("No resources supplied"))
    roots.foldLeft(empty) { (acc, root) =>
      acc.recoverWith {
        // If the previous attempt at finding a device descriptor failed, go to the next one.
        case _: Exception => for {
          deviceDescriptorResource <- findDeviceDescriptorResource(root)
          _ <- canWriteDeviceDescriptorResource(deviceDescriptorResource)
          deviceDescriptor <- loadDeviceDescriptor(deviceDescriptorResource)
        } yield (deviceDescriptor, root)
      }
    }
  }

  private def loadDeviceDescriptor(deviceDescriptorResource: R)
                                  (implicit resource: Resource[R],
                                   resourceStreamProvider: ResourceStreamProvider[R]): Try[DeviceDescriptor] = {
    resource.readFrom(deviceDescriptorResource, in => {
      jsonCodec.parseDeviceDescriptor(Source.fromInputStream(in).mkString)
    })

  }
  /**
    * @inheritdoc
    */
  override def reloadDeviceDescriptor(location: R)
                                     (implicit resource: Resource[R],
                                      resourceStreamProvider: ResourceStreamProvider[R]): Try[DeviceDescriptor] = {
    resource.find(location, RelativePath(DESCRIPTOR_FILENAME)) match {
      case Some(deviceDescriptorResource) => loadDeviceDescriptor(deviceDescriptorResource)
      case None => Try(throw new IllegalArgumentException(s"Cannot find a device descriptor at $location"))
    }
  }
}