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

package devsync.scalafx

import java.io.{InputStream, OutputStream}
import java.nio.file.{FileAlreadyExistsException, Files, Path}

import devsync.json.RelativePath
import devsync.sync.{Resource, ResourceStreamProvider}

import scala.util.Try

/**
  * The typeclass that allows [[Path]]s to be used as [[Resource]]s.
  **/
object PathResource {

  /**
    * The typeclass that allows [[Path]]s to be used as [[Resource]]s.
    **/
  implicit object PathResourceImplicits extends Resource[Path] with ResourceStreamProvider[Path] {

    /**
      * @inheritdoc
      */
    override def canWrite(path: Path): Boolean = Files.isWritable(path)

    /**
      * @inheritdoc
      */
    override def exists(path: Path): Boolean = Files.exists(path)

    /**
      * @inheritdoc
      */
    override def find(path: Path, relativePath: RelativePath): Option[Path] = {
      val newPath = relativePath.pathSegments.foldLeft(path)(_.resolve(_))
      Some(newPath).filter(Files.exists(_))
    }

    /**
      * @inheritdoc
      */
    override def findOrCreateResource(path: Path, mimeType: String, name: String): Try[Path] = {
      Try {
        val newPath: Path = path.resolve(name)
        Some(newPath).filter(Files.exists(_)).getOrElse(Files.createFile(newPath))
      }
    }

    /**
      * @inheritdoc
      */
    override def mkdir(path: Path, name: String): Try[Path] = {
      Try {
        val dir: Path = path.resolve(name)
        if (Files.exists(dir)) {
          if (Files.isDirectory(dir)) {
            dir
          }
          else {
            throw new FileAlreadyExistsException(s"$dir already exists and is not a directory")
          }
        }
        else {
          Files.createDirectory(dir)
        }
      }
    }

    /**
      * @inheritdoc
      */
    override def remove(path: Path): Unit = {
      Try(Files.deleteIfExists(path)).getOrElse({})
    }

    /**
      * @inheritdoc
      */
    override def parent(path: Path): Option[Path] = {
      Option(path.getParent)
    }

    /**
      * @inheritdoc
      */
    override def isEmpty(path: Path): Boolean = {
      Try {
        !Files.newDirectoryStream(path).iterator().hasNext
      }.getOrElse(false)
    }

    /**
      * @inheritdoc
      */
    override def provideInputStream(path: Path): Try[InputStream] = {
      Try(Files.newInputStream(path))
    }

    /**
      * @inheritdoc
      */
    override def provideOutputStream(path: Path): Try[OutputStream] = {
      Try(Files.newOutputStream(path))
    }

  }
}