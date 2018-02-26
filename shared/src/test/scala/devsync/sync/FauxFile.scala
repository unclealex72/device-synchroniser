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

import java.io._

import devsync.json.{DirectoryAndFile, RelativePath}

import scala.collection.mutable

/**
  * Created by alex on 06/04/17
  **/
sealed trait FauxFile extends Ordered[FauxFile] {

  val name: String
  val maybeParent: Option[FauxFile]
  def path: List[String] = printedName :: maybeParent.map(_.path).getOrElse(Nil)

  override def compare(that: FauxFile): Int = name.compare(that.name)

  override def toString: String = path.reverse.mkString("")

  def _prettyPrint(indent: Int): String

  def prettyPrint: String = _prettyPrint(0)

  def printedName: String

  def flatten: Seq[String]
}

case class Directory(
              override val name: String,
              children: mutable.SortedSet[FauxFile] = mutable.SortedSet.empty,
              override val maybeParent: Option[FauxFile] = None) extends FauxFile {

  def createFile(mimeType: String, name: String, content: Option[String] = None): File = {
    create(File(mimeType = mimeType, name = name, maybeParent = Some(this), content = content))
  }

  def createDirectory(name: String): Directory = {
    create(Directory(name = name, maybeParent = Some(this)))
  }

  private def create[F <: FauxFile](fauxFile: F) = {
    children += fauxFile
    fauxFile
  }

  def apply(entries: (Directory => FauxFile)*): Directory = {
    children ++= entries.map(entry => entry(this))
    this
  }

  override def _prettyPrint(indent: Int): String =
    s"${Range(0, indent).map(_ => " ").mkString("")}$name\n" +
    children.map(_._prettyPrint(indent + 2)).mkString("\n")

  override def printedName: String = name + "/"

  def flatten: Seq[String] = Seq(s"$this") ++ children.flatMap(_.flatten)
}

case class File(
              mimeType: String,
              override val name: String,
              var content: Option[String] = None,
              override val maybeParent: Option[FauxFile] = None) extends FauxFile {

  override def _prettyPrint(indent: Int): String =
    s"${Range(0, indent).map(_ => " ").mkString("")}$name $mimeType $content"

  override def printedName: String = name

  override def flatten: Seq[String] = Seq(s"$this $mimeType $content")
}

object FauxFile {

  implicit object FauxResource extends Resource[FauxFile] {
    override def canWrite(fauxFile: FauxFile): Boolean = true

    override def exists(fauxFile: FauxFile): Boolean = true

    override def find(fauxFile: FauxFile, path: RelativePath): Option[FauxFile] = {
      (fauxFile, path) match {
        case (d : Directory, DirectoryAndFile(dir, name)) =>
          if (dir.pathSegments.isEmpty) {
            d.children.find(child => name == child.name)
          }
          else {
            for {
              (childName, pushedRelativePath) <- path.push
              fauxChild <- d.children.find(child => child.name == childName)
              result <- find(fauxChild, pushedRelativePath)
            } yield {
              result
            }
          }
        case _ => None
      }
    }

    override def findOrCreateResource(fauxFile: FauxFile, mimeType: String, name: String): Either[Exception, FauxFile] = {
      fauxFile match {
        case d : Directory => Right(d.createFile(mimeType, name))
        case _ => Left(new IOException(s"Cannot create file $name at ${fauxFile.path}"))
      }
    }

    override def mkdir(fauxFile: FauxFile, name: String): Either[Exception, FauxFile] = {
      fauxFile match {
        case d: Directory => d.children.find(child => child.name == name) match {
          case Some(d: Directory) => Right(d)
          case Some(f: File) => Left(new IOException(s"Cannot create directory $name as ${f.path} is not a directory."))
          case None => Right(d.createDirectory(name))
        }
        case f: File => Left(new IOException(s"Cannot create directory $name as ${f.path} is not a directory."))
      }
    }

    override def remove(fauxFile: FauxFile): Unit = {
      fauxFile.maybeParent.foreach {
        case d : Directory => d.children.remove(fauxFile)
        case _ =>
      }
    }

    override def parent(fauxFile: FauxFile): Option[FauxFile] = fauxFile.maybeParent

    override def isEmpty(fauxFile: FauxFile): Boolean = fauxFile match {
      case d : Directory if d.children.isEmpty => true
      case _ => false
    }
  }

  implicit object FauxResourceStreamProvider extends ResourceStreamProvider[FauxFile] {

    override def provideInputStream(fauxFile: FauxFile): Either[Exception, InputStream] = {
      fauxFile match {
        case f : File => Right(new ByteArrayInputStream(f.content.getOrElse("").getBytes("UTF-8")))
        case d : Directory => Left(new IOException(s"Cannot read from a directory ${d.path}"))
      }
    }

    override def provideOutputStream(fauxFile: FauxFile): Either[Exception, OutputStream] = {
      fauxFile match {
        case f : File =>
          Right(new ByteArrayOutputStream() {
            override def close(): Unit = {
              super.close()
              f.content = Some(toString("UTF-8"))
            }
          })
        case d: Directory => Left(new IOException(s"Cannot write to directory ${d.path}"))
      }
    }
  }
}

object d {
  def root: Directory = Directory("")

  def apply(name: String, entries: (Directory => FauxFile)*): Directory => Directory = { d =>
    Directory(name = name, maybeParent = Some(d))(entries :_*)
  }
}

object f {
  def apply(mimeType: String, name: String, content: String): Directory => File = { d =>
    File(mimeType = mimeType, name = name, content = Some(content), maybeParent = Some(d))
  }
}