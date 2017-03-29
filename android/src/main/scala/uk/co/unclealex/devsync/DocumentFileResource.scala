package uk.co.unclealex.devsync

import java.io.{FileNotFoundException, IOException, InputStream}

import android.support.v4.provider.DocumentFile
import devsync.json.RelativePath
import devsync.sync.{IO, Resource}
import android.content.{Context, Intent}
import macroid.ContextWrapper

/**
  * Created by alex on 27/03/17
  **/
class DocumentFileResource(val documentFile: DocumentFile)(implicit contextWrapper: ContextWrapper) extends Resource {
  val context = contextWrapper.bestAvailable

  override def find(path: RelativePath): Option[DocumentFileResource] = {
    val maybeThis: Option[DocumentFileResource] = Some(this)
    path.pathSegments.foldLeft(maybeThis) { (maybeResource, name) =>
      maybeResource.flatMap { resource =>
        Option(resource.documentFile.findFile(name)).map(df => new DocumentFileResource(df))
      }
    }
  }

  override def copy(in: InputStream, relativePath: RelativePath, mimeType: String): Unit = {
    relativePath.maybeName.foreach { name =>
      val parent = relativePath.maybeParent.map(mkdirs).getOrElse(this)
      find(relativePath).map(_.documentFile).foreach { documentFile =>
        documentFile.delete()
      }
      val newDocumentFile = parent.documentFile.createFile(name, mimeType)
      IO.closing(context.getContentResolver.openOutputStream(newDocumentFile.getUri)) { out =>
        IO.copy(in, out)
      }
      context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, newDocumentFile.getUri))
    }
  }

  def mkdirs(relativePath: RelativePath): DocumentFileResource = {
    val newDir = relativePath.pathSegments.foldLeft(this.documentFile) { (dir, name) =>
      Option(dir.findFile(name)) match {
        case Some(existingFile) => if (existingFile.isDirectory) {
          existingFile
        }
        else {
          throw new IOException(s"File $documentFile exists and is not a directory.")
        }
        case None => dir.createDirectory(name)
      }
    }
    new DocumentFileResource(newDir)
  }

  override def open(relativePath: RelativePath): InputStream = {
    find(relativePath).map(_.documentFile.getUri) match {
      case Some(uri) => context.getContentResolver.openInputStream(uri)
      case _ => throw new FileNotFoundException(relativePath.toString)
    }
  }

  override def remove(relativePath: RelativePath): Unit = {
    find(relativePath).map(_.documentFile).foreach { df =>
      df.delete()
    }
  }

  override def canWrite: Boolean = documentFile.canWrite

  override def toString: String = documentFile.getUri.toString
}
