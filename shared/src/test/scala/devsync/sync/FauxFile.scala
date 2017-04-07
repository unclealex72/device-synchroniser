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

case class D(
              override val name: String,
              children: mutable.SortedSet[FauxFile] = mutable.SortedSet.empty,
              override val maybeParent: Option[FauxFile] = None) extends FauxFile {
  
  def createFile(mimeType: String, name: String, content: Option[String] = None): F = {
    create(F(mimeType = mimeType, name = name, maybeParent = Some(this), content = content))
  }

  def createDirectory(name: String): D = {
    create(D(name = name, maybeParent = Some(this)))
  }

  private def create[F <: FauxFile](fauxFile: F) = {
    children += fauxFile
    fauxFile
  }

  def apply(entries: (D => FauxFile)*): D = {
    children ++= entries.map(entry => entry(this))
    this
  }

  override def _prettyPrint(indent: Int): String =
    s"${Range(0, indent).map(_ => " ").mkString("")}$name\n" +
    children.map(_._prettyPrint(indent + 2)).mkString("\n")

  override def printedName: String = name + "/"

  def flatten: Seq[String] = Seq(s"$this") ++ children.flatMap(_.flatten)
}

case class F(
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

    override def find(fauxFile: FauxFile, path: RelativePath): Option[FauxFile] = {
      (fauxFile, path) match {
        case (d : D, DirectoryAndFile(dir, name)) =>
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

    override def findOrCreateFile(fauxFile: FauxFile, mimeType: String, name: String): Either[Exception, FauxFile] = {
      fauxFile match {
        case d : D => Right(d.createFile(mimeType, name))
        case _ => Left(new IOException(s"Cannot create file $name at ${fauxFile.path}"))
      }
    }

    override def mkdir(fauxFile: FauxFile, name: String): Either[Exception, FauxFile] = {
      fauxFile match {
        case d: D => d.children.find(child => child.name == name) match {
          case Some(d: D) => Right(d)
          case Some(f: F) => Left(new IOException(s"Cannot create directory $name as ${f.path} is not a directory."))
          case None => Right(d.createDirectory(name))
        }
        case f: F => Left(new IOException(s"Cannot create directory $name as ${f.path} is not a directory."))
      }
    }

    override def remove(fauxFile: FauxFile): Unit = {
      fauxFile.maybeParent.foreach {
        case d : D => d.children.remove(fauxFile)
        case _ =>
      }
    }

    override def parent(fauxFile: FauxFile): Option[FauxFile] = fauxFile.maybeParent

    override def isEmpty(fauxFile: FauxFile): Boolean = fauxFile match {
      case d : D if d.children.isEmpty => true
      case _ => false
    }
  }
  
  implicit object FauxResourceStreamProvider extends ResourceStreamProvider[FauxFile] {
    
    override def provideInputStream(fauxFile: FauxFile): Either[Exception, InputStream] = {
      fauxFile match {
        case f : F => Right(new ByteArrayInputStream(f.content.getOrElse("").getBytes("UTF-8")))
        case d : D => Left(new IOException(s"Cannot read from a directory ${d.path}"))
      }
    }

    override def provideOutputStream(fauxFile: FauxFile): Either[Exception, OutputStream] = {
      fauxFile match {
        case f : F =>
          Right(new ByteArrayOutputStream() {
            override def close(): Unit = {
              super.close()
              f.content = Some(toString("UTF-8"))
            }
          })
        case d: D => Left(new IOException(s"Cannot write to directory ${d.path}"))
      }
    }
  }
}

object D {
  def root: D = D("")
}

object d {
  def apply(name: String, entries: (D => FauxFile)*): D => D = { d =>
    D(name = name, maybeParent = Some(d))(entries :_*)
  }
}

object f {
  def apply(mimeType: String, name: String, content: String): D => F = { d =>
    F(mimeType = mimeType, name = name, content = Some(content), maybeParent = Some(d))
  }
}