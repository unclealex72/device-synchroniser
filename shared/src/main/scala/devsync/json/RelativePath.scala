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