package devsync.sync

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

import cats.syntax.either._
import devsync.json._

import scala.io.Source
import scala.util.{Failure, Success, Try}

/**
  * Created by alex on 27/03/17
  **/
class Device(deviceDescriptor: DeviceDescriptor, res: Resource, deviceListener: DeviceListener, jsonCodec: JsonCodec) {

  def synchronise(changes: Seq[Change], now: IsoDate): Unit = {
    val pendingChanges = changes.take(deviceDescriptor.offset)
    val changeCount = pendingChanges.size
    val initialIdx: Either[(Exception, Int), Int] = Right(deviceDescriptor.offset)
    val result: Either[(Exception, Int), Int] = pendingChanges.foldLeft(initialIdx) { (tryIdx, change) =>
      tryIdx.flatMap { idx =>
        try {
          deviceListener.ongoing(idx, changeCount, change)
          change match {
            case Addition(relativePath: RelativePath, at: IsoDate, _links: Links) =>
              add(relativePath, _links)
            case Removal(relativePath: RelativePath, at: IsoDate) =>
              remove(relativePath)
          }
          Right(idx + 1)
        }
        catch {
          case e: Exception => Left(e, idx)
        }
      }
    }
    val updatedDeviceDescriptor = result match {
      case Right(_) =>
        deviceListener.completed(changeCount)
        deviceDescriptor.withOffset(0).withLastModified(now)
      case Left((e, failedIdx)) =>
        deviceListener.failed(e)
        deviceDescriptor.withOffset(failedIdx)
    }
    updateDeviceDescriptor(updatedDeviceDescriptor)
  }

  def add(relativePath: RelativePath, _links: Links): Unit = {
    IO.closing(_links.music.openStream()) { in =>
      res.copy(in, relativePath, "audio/mp3")
    }
  }

  def remove(relativePath: RelativePath): Unit = {
    res.remove(relativePath)
  }

  def updateDeviceDescriptor(updatedDeviceDescriptor: DeviceDescriptor): Unit = {
    IO.closing(new ByteArrayInputStream(
      jsonCodec.writeDeviceDescriptor(updatedDeviceDescriptor).getBytes(StandardCharsets.UTF_8))) { in =>
      res.copy(in, RelativePath(".device.json"), "application/json")
    }
  }
}

object Device {

  def findDeviceDescriptor(jsonCodec: JsonCodec, resources: Iterable[Resource]): Try[DeviceDescriptor] = {
    def findDeviceDescriptorResource(resource: Resource): Try[Resource] = {
      resource.find(RelativePath(".device.json")) match {
        case Some(deviceDescriptorResource) => Success(deviceDescriptorResource)
        case _ => Failure(new IllegalStateException(s"Cannot find a device descriptor resource for $resource"))
      }
    }

    def readDeviceDescriptor(resource: Resource): Try[DeviceDescriptor] = {
      IO.closing(resource.open(RelativePath())) { in =>
        jsonCodec.parseDeviceDescriptor(Source.fromInputStream(in).mkString)
      }
    }

    def canWriteDeviceDescriptorResource(deviceDescriptorResource: Resource): Try[Resource] = {
      if (deviceDescriptorResource.canWrite) {
        Success(deviceDescriptorResource)
      }
      else {
        Failure(new IllegalStateException(s"$deviceDescriptorResource is not writeable"))
      }
    }

    val empty: Try[DeviceDescriptor] = Failure(new RuntimeException("No resources supplied"))
    resources.foldLeft(empty) { (acc, resource) =>
      acc.recoverWith {
        // If the previous attempt at finding a device descriptor failed, go to the next one.
        case _: Exception => for {
          deviceDescriptorResource <- findDeviceDescriptorResource(resource)
          _ <- canWriteDeviceDescriptorResource(deviceDescriptorResource)
          deviceDescriptor <- readDeviceDescriptor(deviceDescriptorResource)
        } yield deviceDescriptor
      }
    }
  }
}