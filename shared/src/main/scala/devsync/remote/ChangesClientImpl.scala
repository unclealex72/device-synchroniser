package devsync.remote

import java.io.{ByteArrayOutputStream, OutputStream}
import java.net.{HttpURLConnection, URL}

import devsync.json.IsoDate._
import devsync.json.RelativePath._
import devsync.json.{RelativePath, _}

import scala.annotation.tailrec
import scala.util.{Success, Try}

/**
  * Created by alex on 24/03/17
  **/
class ChangesClientImpl(val jsonCodec: JsonCodec, val baseUrl: URL) extends ChangesClient {
  /**
    * Get the changes for a user since a specific date.
    */
  override def changesSince(user: String, since: IsoDate): Try[Changes] = {
    readUrl(_.parseChanges, "changes" / user / since)
  }

  /**
    * Count the number of changelog items for a user since a specific date
    */
  override def countChangelogSince(user: String, since: IsoDate): Try[Int] = {
    readUrl(_.parseChangelogCount, "changelog" / "count" / user / since).map(_.count)
  }

  /**
    * Get a page of a changelog for a user.
    */
  override def changelog(user: String, page: Int, limit: Int): Try[Changelog] = {
    readUrl(_.parseChangelog, "changelog" / user / Integer.toString(page) / Integer.toString(limit))
  }

  def readUrl[T](parser: JsonCodec => String => Try[T], relativePath: RelativePath): Try[T] = {
    val url = baseUrl / relativePath
    val body: String = readUrlAsString(url)
    parser(jsonCodec)(body)
  }


  def loadUrl(url: URL, out: OutputStream): Try[Unit] = {
    val conn = url.openConnection.asInstanceOf[HttpURLConnection]
    try {
      val in = conn.getInputStream
      val buffer = new Array[Byte](16384)
      @tailrec
      def doStream(total: Int = 0): Int = {
        val n = in.read(buffer)
        if (n == -1)
          total
        else {
          out.write(buffer, 0, n)
          doStream(total + n)
        }
      }
      doStream()
      Success({})
    }
    finally {
      conn.disconnect()
    }
  }

  def readUrlAsString(url: URL): String = {
    val buff = new ByteArrayOutputStream
    loadUrl(url, buff)
    buff.toString("UTF-8")
  }

  override def tags(addition: Addition): Try[Tags] = {
    jsonCodec.parseTags(readUrlAsString(addition.links.tags))
  }

  override def music(addition: Addition, out: OutputStream): Try[Unit] = {
    data(addition, _.music, out)
  }

  override def artwork(addition: Addition, out: OutputStream): Try[Unit] = {
    data(addition, _.artwork, out)
  }

  def data(addition: Addition, urlExtractor: Links => URL, out: OutputStream): Try[Unit] = {
    loadUrl(urlExtractor(addition.links), out)
  }
}
