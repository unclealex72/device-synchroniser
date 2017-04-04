package devsync.sync

import java.io.{InputStream, OutputStream}

import devsync.json.RelativePath

/**
  * Created by alex on 27/03/17
  * A type class used to allow the android file system and a desktop file system in the same way.
  **/
trait Resource[R] {
  def canWrite(resource: R): Boolean

  def find(resource: R, path: RelativePath): Option[R]

  def findOrCreateFile(resource: R, mimeType: String, name: String): Either[Exception, R]

  def mkdirs(resource: R, relativePath: RelativePath): Either[Exception, R]

  def mkdir(resource: R, name: String): Either[Exception, R]

  def writeTo[T](resource: R, block: OutputStream => Either[Exception, T])
             (implicit resourceStreamProvider: ResourceStreamProvider[R]): Either[Exception, T]

  def readFrom[T](resource: R, block: InputStream => Either[Exception, T])
              (implicit resourceStreamProvider: ResourceStreamProvider[R]): Either[Exception, T]

  def remove(resource: R): Unit

}

/**
  * A type class for classes that are required by a file system to provide access to the contents of files.
  * @tparam R
  */
trait ResourceStreamProvider[R] {

  def provideInputStream(resource: R): Either[Exception, InputStream]

  def provideOutputStream(resource: R): Either[Exception, OutputStream]
}