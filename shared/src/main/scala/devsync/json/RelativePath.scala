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
 * Created by alex on 26/12/14.
 */
case class RelativePath(pathSegments: Seq[String] = Seq.empty) {

    override def toString: String = pathSegments.mkString("/")

    def prefixedWith(prefixPathSegments: String*): RelativePath = {
        RelativePath(prefixPathSegments ++ pathSegments)
    }

    def /(segment: String): RelativePath = RelativePath(pathSegments :+ segment)

    def maybeParent: Option[RelativePath] = if (pathSegments.isEmpty) None else Some(RelativePath(pathSegments.dropRight(1)))

    def maybeName: Option[String] = pathSegments.lastOption

    def push: Option[(String, RelativePath)] = {
      pathSegments.headOption.map(head => (head, RelativePath(pathSegments.tail)))
    }
}

object RelativePath {

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

    implicit class FileExtensions(file: File) {
        def /(relativePath: RelativePath): File = relativePath.pathSegments.foldLeft(file) { (f, segment) =>
            new File(f, segment)
        }
    }

    implicit class UrlExtensions(url: URL) {
        def /(relativePath: RelativePath): URL = new URL(join(url.toString, relativePath.toString))
    }

  implicit class StringExtensions(str: String) {
    def /(next: String): RelativePath = RelativePath(join(str, next))
  }
}

object DirectoryAndFile {
  /**
    * Allow decomposition into a directory and filename
    * @param relativePath
    * @return
    */
  def unapply(relativePath: RelativePath): Option[(RelativePath, String)] = {
    for {
      dir <- relativePath.maybeParent
      name <- relativePath.maybeName
    } yield (dir, name)
  }
}

trait HasRelativePath {

    val relativePath: RelativePath
}
