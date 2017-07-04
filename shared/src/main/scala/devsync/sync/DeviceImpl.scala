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
import cats.instances.future._
import com.typesafe.scalalogging.StrictLogging
import devsync.json._
import devsync.remote.ChangesClient
import org.threeten.bp.Clock

import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source

/**
  * The default implementation of [[Device]]
  * @param jsonCodec The [[JsonCodec]] used to decode JSON objects from the Flac Manager server.
  * @param clock The [[Clock]] used to get the current time.
  * @tparam R The type of files a device contains. There will need to be typeclasses for both `Resource[R]` and
  *           `ResourceStreamProvider[R]`. This then allows the Android filesystem and the Linux filesystem to
  *           be treated as one.
  */
class DeviceImpl[R](jsonCodec: JsonCodec,
                    clock: Clock) extends Device[R] with StrictLogging {

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
                                                               executionContext: ExecutionContext): Future[Either[(Exception, Option[Int]), Int]] = {
    Future(deviceListener.synchronisingStarting())
    reloadDeviceDescriptor(root) match {
      case Right(deviceDescriptor) =>
        new Synchroniser(root, changesClient, deviceListener, deviceDescriptor).synchronise.map { result =>
          result.leftMap(ewmi => (ewmi.e, ewmi.maybeIdx))
        }
      case Left(e) =>
        deviceListener.synchronisingFailed(e, None)
        Future.successful(Left(e, None))
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
    * @param executionContext An execution context for running synchronisation in another thread.
    */
  class Synchroniser(root: R, changesClient: ChangesClient, deviceListener: DeviceListener[R], deviceDescriptor: DeviceDescriptor)(implicit resource: Resource[R],
                                                                                                                                   resourceStreamProvider: ResourceStreamProvider[R],
                                                                                                                                   executionContext: ExecutionContext) {

    /**
      * Synchronise the device.
      * @return Eventually either an [[EWMI]] or the number of changes.
      */
    def synchronise: Future[Either[EWMI, Int]] = {
      val wrappedResult = for {
        changes <- loadChanges
        result <- processChanges(changes)
      } yield result
      wrappedResult.value.flatMap { result =>
        Future {
          updateDeviceDescriptor(deviceDescriptor, result) match {
            case Right(_) => result match {
              case Right(count) =>
                logger.info("Synchronising completed successfully.")
                deviceListener.synchronisingFinished(count)
                Right(count)
              case Left(ewmi) =>
                val e = ewmi.e
                val idx = ewmi.maybeIdx
                logger.error(s"Synchronising Failed at index $idx:", e)
                deviceListener.synchronisingFailed(e, idx)
                Left(ewmi)
            }
            case Left(e) =>
              logger.error("Synchronising Failed:", e)
              deviceListener.synchronisingFailed(e, None)
              Left(ExceptionWithMaybeIndex(e, None))
          }
        }
      }

    }

    /**
      * Load the [[Changes]] from a Flac Manager server.
      * @return Eventually either a [[Changes]] object or a failure.
      */
    def loadChanges: EitherT[Future, EWMI, Changes] = EitherT {
      Future {
        changesClient.changesSince(deviceDescriptor.user, deviceDescriptor.extension, deviceDescriptor.maybeLastModified).leftMap {
          e => ExceptionWithMaybeIndex(e)
        }
      }
    }

    /**
      * Process and synchronise a list of [[Change]]s.
      * @param changes The [[Changes]] object downloaded from the Flac Manager server.
      * @return Eventually either the number of changes or a failure.
      */
    def processChanges(changes: Changes): EitherT[Future, EWMI, Int] = {
      val empty: EitherT[Future, EWMI, Unit] = EitherT.right[Future, EWMI, Unit](Future.successful({}))
      val total = changes.changes.size
      val richChangeWithProgressBuilder = RichChangeWithProgress(total)
      val previouslyUntriedChanges = changes.changes.zipWithIndex.drop(deviceDescriptor.maybeOffset.getOrElse(0))
      previouslyUntriedChanges.foldLeft(empty) { (acc, changeWithIndex) =>
        acc.flatMap { _ =>
          val change = changeWithIndex._1
          val idx = changeWithIndex._2
          val eventualRichChange: Future[RichChange] = change match {
            case addition: Addition =>
              val eventualMaybeTags = Future(changesClient.tags(addition).toOption)
              val eventualMaybeArtwork = Future {
                val buff = new ByteArrayOutputStream()
                IO.closingTry(buff)(changesClient.artwork(addition, _)).toOption.map(_ => buff.toByteArray)
              }
              for {
                maybeTags <- eventualMaybeTags
                maybeArtwork <- eventualMaybeArtwork
              } yield {
                RichAddition(addition, maybeTags, maybeArtwork)
              }
            case removal: Removal => Future.successful(RichRemoval(removal))
          }
          val eventualRichChangeWithProgress = eventualRichChange.map { richChange =>
            richChangeWithProgressBuilder(richChange, idx)
          }
          EitherT.right(eventualRichChangeWithProgress).flatMap(processRichChangeWithProgress).leftMap { e =>
            ExceptionWithMaybeIndex(e, Some(idx))
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
      * @param number The index of this change.
      * @param total The total number of changes.
      */
    case class RichChangeWithProgress(richChange: RichChange, number: Int, total: Int)

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
          RichChangeWithProgress(change, number, total)
      }
    }

    /**
      * Process a [[RichChangeWithProgress]] by either adding or removing the track from the device and notifying a
      * user through the [[DeviceListener]]
      * @param richChangeWithProgress The change to process.
      * @return Eventually [[Unit]] or an exception.
      */
    def processRichChangeWithProgress(richChangeWithProgress: RichChangeWithProgress): EitherT[Future, Exception, Unit] = {
      // When adding and removing music we need to make sure that the addingMusic and removingMusic events are
      // always called before musicAdded and musicRemoved.
      richChangeWithProgress.richChange match {
        case RichAddition(addition, maybeTags, maybeArtwork) =>
          val eventuallyAddingMusic = Future(deviceListener.addingMusic(
            addition, maybeTags, maybeArtwork, richChangeWithProgress.number, richChangeWithProgress.total))
          for {
            file <- addMusic(addition)
            _ <- EitherT.right(eventuallyAddingMusic)
            _ <- EitherT.right(Future(deviceListener.musicAdded(
              addition, maybeTags, maybeArtwork, richChangeWithProgress.number, richChangeWithProgress.total, file)))
          } yield {}
        case RichRemoval(removal) =>
          val eventuallyRemovingMusic = Future(deviceListener.removingMusic(
            removal, richChangeWithProgress.number, richChangeWithProgress.total))
          for {
            _ <- removeMusic(removal)
            _ <- EitherT.right(eventuallyRemovingMusic)
            _ <- EitherT.right(Future(deviceListener.musicRemoved(
              removal, richChangeWithProgress.number, richChangeWithProgress.total)))
          } yield {}
      }
    }

    /**
      * Add a track to the device.
      * @param addition The track to add.
      * @return Eventually either the resource that was newly created or an exception.
      */
    def addMusic(addition: Addition): EitherT[Future, Exception, R] = {
      addition.relativePath match {
        case rp @ DirectoryAndFile(dir, name) =>
          logger.info(s"Adding $rp")
          for {
            directory <- EitherT(Future.successful(resource.mkdirs(root, dir)))
            file <- EitherT(Future.successful(resource.findOrCreateResource(directory, "audio/mp3", name)))
            _ <- EitherT(Future.successful(resource.writeTo(file, out => changesClient.music(addition, out))))
          } yield {
            file
          }
        case _ =>
          EitherT.left[Future, Exception, R](
            Future.successful(
              new IllegalArgumentException(s"Relative path ${addition.relativePath} does not point to a file and directory.")))
      }
    }

    /**
      * Remove a track from the device.
      * @param removal The track to remove.
      * @return Eventuall either [[Unit]] or an exception.
      */
    def removeMusic(removal: Removal): EitherT[Future, Exception, Unit] = {
      val path = removal.relativePath
      logger.info(s"Removing $path")
      EitherT.right[Future, Exception, Unit] {
        Future.successful {
          resource.find(root, path).foreach(resource.removeAndCleanDirectories)
        }
      }
    }

    /**
      * Update the device descriptor with the result of this synchronisation.
      * @param deviceDescriptor The device descriptor to update.
      * @param accumulatedResult The number of changes that were synchronised or an exception with maybe an index.
      * @return Either [[Unit]] or an exception if the device descriptor could not be saved back to the device.
      */
    def updateDeviceDescriptor(deviceDescriptor: DeviceDescriptor, accumulatedResult: Either[EWMI, Int]): Either[Exception, Unit] = {
      // Finished - log whether synchronisation was successful or not and write the new device descriptor back to the
      val newDeviceDescriptor = accumulatedResult match {
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
                                    resourceStreamProvider: ResourceStreamProvider[R]): Either[Exception, (DeviceDescriptor, R)] = {
    def findDeviceDescriptorResource(root: R): Either[Exception, R] = {
      resource.find(root, RelativePath(DESCRIPTOR_FILENAME)) match {
        case Some(deviceDescriptorResource) => Right(deviceDescriptorResource)
        case _ => Left(new IllegalStateException(s"Cannot find a device descriptor resource for $root"))
      }
    }

    def canWriteDeviceDescriptorResource(deviceDescriptorResource: R): Either[Exception, R] = {
      if (resource.canWrite(deviceDescriptorResource)) {
        Right(deviceDescriptorResource)
      }
      else {
        Left(new IllegalStateException(s"$deviceDescriptorResource is not writeable"))
      }
    }

    val empty: Either[Exception, (DeviceDescriptor, R)] = Left(new RuntimeException("No resources supplied"))
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
                                   resourceStreamProvider: ResourceStreamProvider[R]): Either[Exception, DeviceDescriptor] = {
    resource.readFrom(deviceDescriptorResource, in => {
      jsonCodec.parseDeviceDescriptor(Source.fromInputStream(in).mkString)
    })

  }
  /**
    * @inheritdoc
    */
  override def reloadDeviceDescriptor(location: R)
                                     (implicit resource: Resource[R],
                                      resourceStreamProvider: ResourceStreamProvider[R]): Either[Exception, DeviceDescriptor] = {
    resource.find(location, RelativePath(DESCRIPTOR_FILENAME)) match {
      case Some(deviceDescriptorResource) => loadDeviceDescriptor(deviceDescriptorResource)
      case None => Left(new IllegalArgumentException(s"Cannot find a device descriptor at $location"))
    }
  }
}