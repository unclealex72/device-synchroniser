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

import java.io.{InputStream, OutputStream}

import cats.syntax.either._
import com.typesafe.scalalogging.StrictLogging
import devsync.json.RelativePath

import scala.util.Try

/**
  * A type class used to allow the android file system and a desktop file system to be accessed in the same way.
  **/
trait Resource[R] extends StrictLogging {

  /**
    * Check to see if a resource can be written to.
    * @param resource The resource to check.
    * @return True if the resource can be written to, false otherwise.
    */
  def canWrite(resource: R): Boolean

  /**
    * Check to see if a resource exists.
    * @param resource The resource to check.
    * @return True if the resource can exists, false otherwise.
    */
  def exists(resource: R): Boolean

  /**
    * Try and find a resource.
    * @param resource The base resource.
    * @param path A relative path relative to the base resource.
    * @return The resource at the new path or none if no such resource could be found.
    */
  def find(resource: R, path: RelativePath): Option[R]

  /**
    * Find or create a new resource.
    * @param resource The base resource.
    * @param mimeType The mime type of the new resource.
    * @param name The name of the new resource.
    * @return Either the new resource or an exception.
    */
  def findOrCreateResource(resource: R, mimeType: String, name: String): Try[R]

  /**
    * Create a new directory or do nothing if the directory already exists.
    * @param resource The base resource.
    * @param name The name of the new directory.
    * @return Either an exception or a new resource for the directory.
    */
  def mkdir(resource: R, name: String): Try[R]

  /**
    * Create a hierarchy of directories or do nothing if the hierarchy already exists.
    * @param resource The base resource.
    * @param relativePath The relative path of the new directory.
    * @return Either an exception or a new resource for the directory.
    */
  def mkdirs(resource: R, relativePath: RelativePath): Try[R] = {
    val empty: Try[R] = Try(resource)
    relativePath.pathSegments.foldLeft(empty) { (acc, name) =>
      acc.flatMap(mkdir(_, name))
    }
  }

  /**
    * Write to a resource.
    * @param resource The resource to write to.
    * @param block A block of code that provides data to an output stream.
    * @param resourceStreamProvider The resource stream provider used to get an output stream for the resource.
    * @tparam T The type of result to return.
    * @return Either the result of executing the block of code or an exception.
    */
  def writeTo[T](resource: R, block: OutputStream => Try[T])
                         (implicit resourceStreamProvider: ResourceStreamProvider[R]): Try[T] = {
    logger.info(s"Opening $resource for writing")
    for {
      out <- resourceStreamProvider.provideOutputStream(resource)
      result <- IO.closingTry(out)(block)
    } yield result
  }

  /**
    * Read from a resource.
    * @param resource The resource to read from.
    * @param block A block of code that receives data from an input stream.
    * @param resourceStreamProvider The resource stream provider used to get an input stream for the resource.
    * @tparam T The type of result to return.
    * @return Either the result of executing the block of code or an exception.
    */
  def readFrom[T](resource: R, block: InputStream => Try[T])
                          (implicit resourceStreamProvider: ResourceStreamProvider[R]): Try[T] = {
    logger.info(s"Opening $resource for reading")
    for {
      in <- resourceStreamProvider.provideInputStream(resource)
      result <- IO.closingTry(in)(block)
    } yield result
  }

  /**
    * Remove a resource.
    * @param resource The resource to remove.
    */
  def remove(resource: R): Unit

  /**
    * Get a resource's parent.
    * @param resource The resource who's parent is being requested.
    * @return Either the resource's parent or none if the resource is a root directory.
    */
  def parent(resource: R): Option[R]

  /**
    * Check to see if a directory resource is empty.
    * @param resource The resource to check.
    * @return True if the resource is an empty directory.
    */
  def isEmpty(resource: R): Boolean

  /**
    * Remove a resource and traverse up it's parents, removing any empty directories.
    * @param resource
    */
  def removeAndCleanDirectories(resource: R): Unit = {
    if (exists(resource)) {
      remove(resource)
      Iterator.iterate(parent(resource))(mr => mr.flatMap(parent)).takeWhile(mr => mr.exists(r => !isEmpty(r))).foreach { mr =>
        mr.foreach(remove)
      }
    }
  }
}

/**
  * A type class for classes that are required by a file system to provide access to the contents of files.
  * @tparam R A resource type.
  */
trait ResourceStreamProvider[R] {

  /**
    * Provide an input stream that gets data from a resource.
    * @param resource The resource to be read.
    * @return Either an input stream for the resource or an exception.
    */
  def provideInputStream(resource: R): Try[InputStream]

  /**
    * Provide an output stream that sends data from to resource.
    * @param resource The resource to be written.
    * @return Either an output stream for the resource or an exception.
    */
  def provideOutputStream(resource: R): Try[OutputStream]
}