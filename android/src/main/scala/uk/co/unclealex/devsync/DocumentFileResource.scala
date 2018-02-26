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

package uk.co.unclealex.devsync

import java.io.{IOException, InputStream, OutputStream}

import android.support.v4.provider.DocumentFile
import cats.syntax.either._
import com.typesafe.scalalogging.StrictLogging
import devsync.json.{DirectoryAndFile, RelativePath}
import devsync.sync.{IO, Resource, ResourceStreamProvider}
import macroid.ContextWrapper

import scala.util.Try

/**
  * A typeclass that allows an Android [[DocumentFile]] to be used as a [[Resource]].
  **/
object DocumentFileResource {

  /**
    * A typeclass that allows an Android [[DocumentFile]] to be used as a [[Resource]].
    */
  implicit object ImplicitDocumentFileResource extends Resource[DocumentFile] with StrictLogging {

    private def findOrCreate(documentFile: DocumentFile,
                             name: String,
                             builder: String => DocumentFile,
                             isRequiredType: DocumentFile => Boolean,
                             wrongTypeMessage: String => String): Either[Exception, DocumentFile]  = {
      Option(documentFile.findFile(name)) match {
        case Some(file) if isRequiredType(file) => Right(file)
        case None =>
          try {
            logger.info(s"Creating $name")
            Right(builder(name))
          }
          catch {
            case e: Exception => Left(e)
          }
        case _ => Left(new IOException(wrongTypeMessage(name)))
      }
    }

    /**
      * @inheritdoc
      */
    override def mkdir(documentFile: DocumentFile, name: String): Either[Exception, DocumentFile] =
      findOrCreate(documentFile, name, documentFile.createDirectory, _.isDirectory, name => s"$name is not a directory")

    /**
      * @inheritdoc
      */
    override def findOrCreateResource(documentFile: DocumentFile, mimeType: String, name: String): Either[Exception, DocumentFile] =
      findOrCreate(
        documentFile,
        name,
        documentFile.createFile(mimeType, _),
        _.isFile,
        nm => s"$nm is not a standard file")

    /**
      * @inheritdoc
      */
    override def writeTo[T](documentFile: DocumentFile, block: OutputStream => Either[Exception, T])
                        (implicit resourceStreamProvider: ResourceStreamProvider[DocumentFile]): Either[Exception, T] = {
      val uri = documentFile.getUri
      logger.info(s"Opening $uri for writing")
      for {
        out <- resourceStreamProvider.provideOutputStream(documentFile)
        result <- IO.closingTry(out)(block)
      } yield result
    }

    /**
      * @inheritdoc
      */
    override def readFrom[T](documentFile: DocumentFile, block: InputStream => Either[Exception, T])
                         (implicit resourceStreamProvider: ResourceStreamProvider[DocumentFile]): Either[Exception, T] = {
      val uri = documentFile.getUri
      logger.info(s"Opening $uri for reading")
      for {
        in <- resourceStreamProvider.provideInputStream(documentFile)
        result <- IO.closingTry(in)(block)
      } yield result
    }

    /**
      * @inheritdoc
      */
    override def remove(documentFile: DocumentFile): Unit = {
      logger.info(s"Removing ${documentFile.getUri}")
      Try(documentFile.delete())
    }

    /**
      * @inheritdoc
      */
    override def canWrite(documentFile: DocumentFile): Boolean = documentFile.canWrite

    /**
      * @inheritdoc
      */
    override def exists(documentFile: DocumentFile): Boolean = documentFile.exists

    /**
      * @inheritdoc
      */
    override def find(documentFile: DocumentFile, path: RelativePath): Option[DocumentFile] = {
      path match {
        case DirectoryAndFile(dir, name) =>
          val directoryPathSegments = dir.pathSegments
          if (directoryPathSegments.isEmpty) {
            Option(documentFile.findFile(name))
          }
          else {
            val maybeChildDirName = directoryPathSegments.headOption
            val tail = RelativePath(directoryPathSegments.tail) / name
            for {
              childDirName <- maybeChildDirName
              childDir <- Option(documentFile.findFile(childDirName))
              file <- find(childDir, tail)
            } yield file
          }
        case _ => None
      }
    }

    /**
      * @inheritdoc
      */
    override def parent(documentFile: DocumentFile): Option[DocumentFile] = Option(documentFile.getParentFile)

    /**
      * @inheritdoc
      */
    override def isEmpty(documentFile: DocumentFile): Boolean =
      Option(documentFile.listFiles()).exists(_.isEmpty)
  }

  /**
    * A typeclass that allows an Android [[DocumentFileResource]] to be used as a [[ResourceStreamProvider]]
    * @param contextWrapper A context wrapper used to resolve content from a URI.
    */
  class DocumentFileResourceStreamProvider(implicit contextWrapper: ContextWrapper) extends
    ResourceStreamProvider[DocumentFile] {

    /**
      * @inheritdoc
      */
    override def provideInputStream(resource: DocumentFile): Either[Exception, InputStream] = {
      try {
        Right(contextWrapper.bestAvailable.getContentResolver.openInputStream(resource.getUri))
      }
      catch {
        case e: Exception => Left(e)
      }
    }

    /**
      * @inheritdoc
      */
    override def provideOutputStream(resource: DocumentFile): Either[Exception, OutputStream] = {
      try {
        Right(contextWrapper.bestAvailable.getContentResolver.openOutputStream(resource.getUri))
      }
      catch {
        case e: Exception => Left(e)
      }
    }
  }
}
