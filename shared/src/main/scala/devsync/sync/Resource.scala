package devsync.sync

import java.io.{InputStream, OutputStream}

import cats.syntax.either._
import com.typesafe.scalalogging.StrictLogging
import devsync.json.RelativePath

/**
  * Created by alex on 27/03/17
  * A type class used to allow the android file system and a desktop file system in the same way.
  **/
trait Resource[R] extends StrictLogging {
  def canWrite(resource: R): Boolean

  def find(resource: R, path: RelativePath): Option[R]

  def findOrCreateFile(resource: R, mimeType: String, name: String): Either[Exception, R]

  def mkdir(resource: R, name: String): Either[Exception, R]

  def mkdirs(resource: R, relativePath: RelativePath): Either[Exception, R] = {
    val empty: Either[Exception, R] = Right(resource)
    relativePath.pathSegments.foldLeft(empty) { (acc, name) =>
      acc.flatMap(mkdir(_, name))
    }
  }

  def writeTo[T](resource: R, block: OutputStream => Either[Exception, T])
                         (implicit resourceStreamProvider: ResourceStreamProvider[R]): Either[Exception, T] = {
    logger.info(s"Opening $resource for writing")
    for {
      out <- resourceStreamProvider.provideOutputStream(resource)
      result <- IO.closingTry(out)(block)
    } yield result
  }

  def readFrom[T](resource: R, block: InputStream => Either[Exception, T])
                          (implicit resourceStreamProvider: ResourceStreamProvider[R]): Either[Exception, T] = {
    logger.info(s"Opening $resource for reading")
    for {
      in <- resourceStreamProvider.provideInputStream(resource)
      result <- IO.closingTry(in)(block)
    } yield result
  }

  def remove(resource: R): Unit

  def parent(resource: R): Option[R]

  def isEmpty(resource: R): Boolean

  def removeAndCleanDirectories(resource: R): Unit = {
    remove(resource)
    Iterator.iterate(parent(resource))(mr => mr.flatMap(parent)).takeWhile(mr => mr.exists(r => !isEmpty(r))).foreach { mr =>
      mr.foreach(remove)
    }
  }
}

/**
  * A type class for classes that are required by a file system to provide access to the contents of files.
  * @tparam R
  */
trait ResourceStreamProvider[R] {

  def provideInputStream(resource: R): Either[Exception, InputStream]

  def provideOutputStream(resource: R): Either[Exception, OutputStream]
}