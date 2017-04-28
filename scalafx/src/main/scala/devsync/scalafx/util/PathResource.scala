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

package devsync.scalafx.util

import java.io.{InputStream, OutputStream}
import java.nio.file.{FileAlreadyExistsException, Files, Path}

import cats.syntax.either._
import devsync.json.RelativePath
import devsync.sync.{Resource, ResourceStreamProvider}

/**
  * Created by alex on 09/04/17
  **/
object PathResource {

  implicit object PathResourceImplicits extends Resource[Path] with ResourceStreamProvider[Path] {

    override def canWrite(path: Path): Boolean = Files.isWritable(path)

    override def find(path: Path, relativePath: RelativePath): Option[Path] = {
      val newPath = relativePath.pathSegments.foldLeft(path)(_.resolve(_))
      Some(newPath).filter(Files.exists(_))
    }

    override def findOrCreateFile(path: Path, mimeType: String, name: String): Either[Exception, Path] = {
      tryIO {
        val newPath = path.resolve(name)
        Some(newPath).filter(Files.exists(_)).getOrElse(Files.createFile(newPath))
      }
    }

    override def mkdir(path: Path, name: String): Either[Exception, Path] = {
      liftTryIO{
        val dir = path.resolve(name)
        if (Files.exists(dir)) {
          if (Files.isDirectory(dir)) {
            Right(dir)
          }
          else {
            Left(new FileAlreadyExistsException(s"$dir already exists and is not a directory"))
          }
        }
        else {
          Right(Files.createDirectory(dir))
        }
      }
    }

    override def remove(path: Path): Unit = {
      tryIO(Files.deleteIfExists(path)).getOrElse({})
    }

    override def parent(path: Path): Option[Path] = {
      Option(path.getParent)
    }

    override def isEmpty(path: Path): Boolean = {
      tryIO {
        !Files.newDirectoryStream(path).iterator().hasNext
      }.getOrElse(false)
    }

    override def provideInputStream(path: Path): Either[Exception, InputStream] = {
      tryIO(Files.newInputStream(path))
    }

    override def provideOutputStream(path: Path): Either[Exception, OutputStream] = {
      tryIO(Files.newOutputStream(path))
    }

    def tryIO[V](block: => V): Either[Exception, V] = liftTryIO(Right(block))

    def liftTryIO[V](block: => Either[Exception, V]): Either[Exception, V] = {
      try {
        block
      }
      catch {
        case e: Exception => Left(e)
      }
    }
  }
}