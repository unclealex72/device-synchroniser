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
  * Created by alex on 02/04/17
  **/
object DocumentFileResource {

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

    def mkdir(documentFile: DocumentFile, name: String): Either[Exception, DocumentFile] =
      findOrCreate(documentFile, name, documentFile.createDirectory, _.isDirectory, name => s"$name is not a directory")

    def findOrCreateFile(documentFile: DocumentFile, mimeType: String, name: String): Either[Exception, DocumentFile] =
      findOrCreate(
        documentFile,
        name,
        documentFile.createFile(mimeType, _),
        _.isFile,
        nm => s"$nm is not a standard file")

    override def writeTo[T](documentFile: DocumentFile, block: OutputStream => Either[Exception, T])
                        (implicit resourceStreamProvider: ResourceStreamProvider[DocumentFile]): Either[Exception, T] = {
      val uri = documentFile.getUri
      logger.info(s"Opening $uri for writing")
      for {
        out <- resourceStreamProvider.provideOutputStream(documentFile)
        result <- IO.closingTry(out)(block)
      } yield result
    }

    override def readFrom[T](documentFile: DocumentFile, block: InputStream => Either[Exception, T])
                         (implicit resourceStreamProvider: ResourceStreamProvider[DocumentFile]): Either[Exception, T] = {
      val uri = documentFile.getUri
      logger.info(s"Opening $uri for reading")
      for {
        in <- resourceStreamProvider.provideInputStream(documentFile)
        result <- IO.closingTry(in)(block)
      } yield result
    }

    def remove(documentFile: DocumentFile): Unit = {
      logger.info(s"Removing ${documentFile.getUri}")
      Try(documentFile.delete())
    }

    override def canWrite(documentFile: DocumentFile): Boolean = documentFile.canWrite

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

    override def parent(documentFile: DocumentFile): Option[DocumentFile] = Option(documentFile.getParentFile)

    override def isEmpty(documentFile: DocumentFile): Boolean =
      Option(documentFile.listFiles()).exists(_.isEmpty)
  }

  class DocumentFileResourceStreamProvider(implicit contextWrapper: ContextWrapper) extends ResourceStreamProvider[DocumentFile] {

    override def provideInputStream(resource: DocumentFile): Either[Exception, InputStream] = {
      try {
        Right(contextWrapper.bestAvailable.getContentResolver.openInputStream(resource.getUri))
      }
      catch {
        case e: Exception => Left(e)
      }
    }

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
