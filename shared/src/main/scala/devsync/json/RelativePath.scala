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

package devsync.json

import java.io.File
import java.net.URL

/**
  * A relative path is used to locate an album or a track relative to the root directory of a device.
  * @param pathSegments A list of path segments to make up a relative path.
  */
case class RelativePath(pathSegments: Seq[String] = Seq.empty) {

  /**
    * Convert this relative path to a string.
    * @return A string of the form `directory/subdirectory/file`.
    */
  override def toString: String = pathSegments.mkString("/")

  /**
    * Prepend a list of path segments to a relative path.
    * @param prefixPathSegments The path segments to prefix.
    * @return A new relative path of the form `prefix1/prefix2/directory/subdirectory/file`.
    */
  def prefixedWith(prefixPathSegments: String*): RelativePath = {
      RelativePath(prefixPathSegments ++ pathSegments)
  }

  /**
    * Add a new path segment.
    * @param segment The segment to add.
    * @return A new relative path of the form `directory/subdirectory/newSegment`.
    */
  def /(segment: String): RelativePath = RelativePath(pathSegments :+ segment)

  /**
    * Get the parent of a relative path if it has one.
    * @return The relative path's parent or none if this relative path is empty.
    */
  def maybeParent: Option[RelativePath] = if (pathSegments.isEmpty) None else Some(RelativePath(pathSegments.dropRight(1)))

  /**
    * Get the file name of a relative path if it has one.
    * @return The final segment of this relative path or none if the relative path is empty.
    */
  def maybeName: Option[String] = pathSegments.lastOption

  /**
    * Split a path of the form `directory/subdirectory/file` into `directory` and `subdirectory/file` if it is not empty.
    * @return A pair of the top level path segment and a tail or none if this relative path is empty.
    */
  def push: Option[(String, RelativePath)] = {
    pathSegments.headOption.map(head => (head, RelativePath(pathSegments.tail)))
  }
}

/**
  * An object to help with creating relative paths.
  */
object RelativePath {

  /**
    * Create a new relative path from a forward slash delimited string of path segments.
     * @param path A string of the form `directory/subdirectory/filename`.
    * @return A new relative path.
    */
  def apply(path: String): RelativePath = {
      RelativePath(path.split('/').filterNot(_.isEmpty))
  }

  private def join(left: String, right: String): String = {
      (left.endsWith("/"), right.startsWith("/")) match {
          case (true, true) => left + right.substring(1)
          case (false, false) => s"$left/$right"
          case _ => left + right
      }
  }

  /**
    * An implicit class that can be used to append relative paths to a [[File]]
    * @param file The file to extend.
    */
  implicit class FileExtensions(file: File) {

    /**
      * Create a new file that is the combination of the file and the relative path.
      * @param relativePath The relative path to traverse.
      * @return A new file that points to a new absolute path (relative to the original file).
      */
    def /(relativePath: RelativePath): File = relativePath.pathSegments.foldLeft(file) { (f, segment) =>
        new File(f, segment)
    }
  }

  /**
    * An implicit class that can be used to append relative paths to a [[URL]]
    * @param url The URL to extend.
    */
  implicit class UrlExtensions(url: URL) {

    /**
      * Create a new URL that is the combination of the URL and the relative path.
      * @param relativePath The relative path to traverse.
      * @return A new URL that points to the a new absolute path (relative to the URL).
      */
      def /(relativePath: RelativePath): URL = new URL(join(url.toString, relativePath.toString))
  }

  /**
    * An implicit class that allows relative paths to be created from strings.
    * @param str The string to extend.
    */
  implicit class StringExtensions(str: String) {

    /**
      * Create a relative path of the form `str/next`
      * @param next The path segment to add.
      * @return A new relative path.
      */
    def /(next: String): RelativePath = RelativePath(join(str, next))
  }
}

/**
  * An object to allow a relative path to be decomposed into a directory and filename.
  */
object DirectoryAndFile {
  /**
    * Allow decomposition into a directory and filename
    * @param relativePath The relative path to attempt to decompose.
    * @return A relative path for the directory and a filename or none if the original relative path was empty.
    */
  def unapply(relativePath: RelativePath): Option[(RelativePath, String)] = {
    for {
      dir <- relativePath.maybeParent
      name <- relativePath.maybeName
    } yield (dir, name)
  }
}

/**
  * A trait for classes that include a relative path.
  */
trait HasRelativePath {

  /**
    * A relative path, usually of a music track or album.
    */
  val relativePath: RelativePath
}
