package devsync.sync

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import cats.data.EitherT
import cats.instances.future._
import cats.syntax.either._
import com.typesafe.scalalogging.StrictLogging
import devsync.json._
import devsync.remote.ChangesClient

import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source
/**
  * Created by alex on 27/03/17apply
  **/
class DeviceImpl[R](jsonCodec: JsonCodec, isoClock: IsoClock) extends Device[R] with StrictLogging {

  val DESCRIPTOR_FILENAME = "device.json"

  case class ExceptionWithMaybeIndex(e: Exception, maybeIdx: Option[Int] = None)
  type EWMI = ExceptionWithMaybeIndex

  override def synchronise(
                            root: R,
                            changesClient: ChangesClient,
                            deviceListener: DeviceListener[R])
                          (implicit resource: Resource[R],
                           resourceStreamProvider: ResourceStreamProvider[R],
                           ec: ExecutionContext): Future[Either[(Exception, Option[Int]), Int]] = {
    Future(deviceListener.synchronisingStarting())
    findDeviceDescriptor(Seq(root)) match {
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
    * Keep track of the index of the change that failed (if, any)
    * @param e
    * @param maybeIdx
    */
  class Synchroniser(root: R, changesClient: ChangesClient, deviceListener: DeviceListener[R], deviceDescriptor: DeviceDescriptor)
                    (implicit resource: Resource[R], resourceStreamProvider: ResourceStreamProvider[R], ec: ExecutionContext) {

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


    def loadChanges: EitherT[Future, EWMI, Changes] = EitherT {
      Future {
        changesClient.changesSince(deviceDescriptor.user, deviceDescriptor.maybeLastModified).leftMap {
          e => ExceptionWithMaybeIndex(e)
        }
      }
    }

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

    def addMusic(addition: Addition): EitherT[Future, Exception, R] = {
      addition.relativePath match {
        case rp @ DirectoryAndFile(dir, name) =>
          logger.info(s"Adding $rp")
          for {
            directory <- EitherT(Future.successful(resource.mkdirs(root, dir)))
            file <- EitherT(Future.successful(resource.findOrCreateFile(directory, "audio/mp3", name)))
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

    def removeMusic(removal: Removal): EitherT[Future, Exception, Unit] = {
      val path = removal.relativePath
      logger.info(s"Removing $path")
      EitherT.right[Future, Exception, Unit] {
        Future.successful {
          resource.find(root, path).foreach(resource.removeAndCleanDirectories)
        }
      }
    }

    sealed trait RichChange
    case class RichAddition(addition: Addition, maybeTags: Option[Tags], maybeArtwork: Option[Array[Byte]]) extends RichChange
    case class RichRemoval(removal: Removal) extends RichChange

    case class RichChangeWithProgress(richChange: RichChange, number: Int, total: Int)
    object RichChangeWithProgress {
      def apply(total: Int): (RichChange, Int) => RichChangeWithProgress = {
        case (change, number) =>
          RichChangeWithProgress(change, number, total)
      }
    }

    def updateDeviceDescriptor(deviceDescriptor: DeviceDescriptor, accumulatedResult: Either[EWMI, Int]): Either[Exception, Unit] = {
      // Finished - log whether synchronisation was successful or not and write the new device descriptor back to the
      val newDeviceDescriptor = accumulatedResult match {
        case Right(_) =>
          deviceDescriptor.copy(maybeLastModified = Some(isoClock.now), maybeOffset = None)
        case Left(ewmi) =>
          // If the failed index is empty, keep the old index.
          logger.error(s"Synchronising failed at index ${ewmi.maybeIdx}", ewmi.e)
          deviceDescriptor.copy(maybeOffset = ewmi.maybeIdx.orElse(deviceDescriptor.maybeOffset))
      }
      for {
        deviceDescriptorFile <- resource.findOrCreateFile(root, "application/json", DESCRIPTOR_FILENAME)
        _ <- resource.writeTo(deviceDescriptorFile, out => {
          IO.closing(new ByteArrayInputStream(jsonCodec.writeDeviceDescriptor(newDeviceDescriptor).getBytes("UTF-8"))) { in =>
            IO.copy(in, out)
          }
        })
      } yield {}
    }
  }

  override def findDeviceDescriptor(roots: Iterable[R])
                                   (implicit resource: Resource[R],
                                    resourceStreamProvider: ResourceStreamProvider[R]): Either[Exception, DeviceDescriptor] = {
    def findDeviceDescriptorResource(root: R): Either[Exception, R] = {
      resource.find(root, RelativePath(DESCRIPTOR_FILENAME)) match {
        case Some(deviceDescriptorResource) => Right(deviceDescriptorResource)
        case _ => Left(new IllegalStateException(s"Cannot find a device descriptor resource for $root"))
      }
    }

    def readDeviceDescriptor(deviceDescriptorResource: R): Either[Exception, DeviceDescriptor] = {
      resource.readFrom(deviceDescriptorResource, in => {
        jsonCodec.parseDeviceDescriptor(Source.fromInputStream(in).mkString)
      })
    }

    def canWriteDeviceDescriptorResource(deviceDescriptorResource: R): Either[Exception, R] = {
      if (resource.canWrite(deviceDescriptorResource)) {
        Right(deviceDescriptorResource)
      }
      else {
        Left(new IllegalStateException(s"$deviceDescriptorResource is not writeable"))
      }
    }

    val empty: Either[Exception, DeviceDescriptor] = Left(new RuntimeException("No resources supplied"))
    roots.foldLeft(empty) { (acc, root) =>
      acc.recoverWith {
        // If the previous attempt at finding a device descriptor failed, go to the next one.
        case _: Exception => for {
          deviceDescriptorResource <- findDeviceDescriptorResource(root)
          _ <- canWriteDeviceDescriptorResource(deviceDescriptorResource)
          deviceDescriptor <- readDeviceDescriptor(deviceDescriptorResource)
        } yield deviceDescriptor
      }
    }
  }
}