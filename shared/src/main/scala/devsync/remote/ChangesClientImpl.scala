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

package devsync.remote

import java.io.{ByteArrayOutputStream, OutputStream}
import java.net.{HttpURLConnection, URL}
import java.util.Date

import cats.syntax.either._
import devsync.json.RelativePath._
import devsync.json.{RelativePath, _}
import devsync.logging.PassthroughLogging
import devsync.sync.IO
import org.threeten.bp.{Instant, ZoneId}
import org.threeten.bp.format.DateTimeFormatter


/**
  * The default implementation of [[ChangesClient]]
  * @param jsonCodec The [[JsonCodec]] used to encode and decode JSON objects.
  * @param baseUrl The Flac Manager server URL.
  **/
class ChangesClientImpl(val jsonCodec: JsonCodec, val baseUrl: URL) extends ChangesClient with PassthroughLogging {

  private val formatter: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.systemDefault())

  implicit def instantToString(instant: Instant): String = formatter.format(instant)

  /**
    * @inheritdoc
    */
  override def changesSince(user: String, extension: Extension, maybeSince: Option[Instant]): Either[Exception, Changes] = {
    val since = orDatum(maybeSince)
    logger.info(s"Looking for changes since $since")
    readUrl(_.parseChanges, "changes" / user / extension.extension / since).error(s"Could not download changes since $since")
  }

  /**
    * @inheritdoc
    */
  override def changelogSince(user: String, extension: Extension, maybeSince: Option[Instant]): Either[Exception, Changelog] = {
    val since = orDatum(maybeSince)
    logger.info(s"Loading the changelog since $since")
    readUrl(_.parseChangelog, "changelog" / user / extension.extension / since).error(s"Could not download the changelog since $since")
  }

  /**
    * Return either a supplied date or the Unix epoch.
    * @param maybeSince The date to return or none if the Unix epoch is to be returned.
    * @return An [[Instant]].
    */
  def orDatum(maybeSince: Option[Instant]): Instant = maybeSince.getOrElse(Instant.ofEpochMilli(0))

  /**
    * Read a JSON object from a path relative to the server URL.
    * @param parser The function used to parse the JSON received.
    * @param relativePath The relative path of the resource on the server.
    * @tparam T The type of JSON object to parse.
    * @return Either a parsed JSON object or an exception.
    */
  def readUrl[T](parser: JsonCodec => String => Either[Exception, T], relativePath: RelativePath): Either[Exception, T] = {
    val url = baseUrl / relativePath
    for {
      body <- readUrlAsString(url)
      result <- parser(jsonCodec)(body)
    } yield result
  }

  /**
    * Copy a URL into an output stream.
    * @param url The URL to load.
    * @param out The output stream to copy the URL's data in to.
    * @param useCache True if the call to the URL should be cached, false otherwise.
    * @return Either a [[Unit]] on successs or an exception otherwise.
    */
  def loadUrl(url: URL, out: OutputStream, useCache: Boolean = true): Either[Exception, Unit] = {
    logger.info(s"Loading url $url")
    val conn = url.openConnection.asInstanceOf[HttpURLConnection]
    if (!useCache) {
      conn.addRequestProperty("Cache-Control", "no-cache")
    }
    IO.closing(conn.getInputStream, conn.disconnect()) { in =>
      IO.copy(in, out)
    }
  }

  /**
    * Read a URL and convert its data to a UTF-8 string.
    * @param url The URL to read.
    * @return Either the URL's data as a string or an exception.
    */
  def readUrlAsString(url: URL): Either[Exception, String] = {
    val buff = new ByteArrayOutputStream
    loadUrl(url, buff).map(_ => buff.toString("UTF-8"))
  }.error(s"Could not read the data from")

  /**
    * @inheritdoc
    */
  override def tags(item: HasLinks with HasRelativePath): Either[Exception, Tags] = {
    for {
      tagsData <- readUrlAsString(item.links.tags)
      tags <- jsonCodec.parseTags(tagsData)
    } yield tags
  }

  /**
    * @inheritdoc
    */
  override def music(item: HasLinks with HasRelativePath, out: OutputStream): Either[Exception, Unit] = {
    logger.info(s"Downloading music for ${item.relativePath}")
    data(item, _.music, out)
  }.error(s"Could not download the music for ${item.relativePath}")

  /**
    * @inheritdoc
    */
  override def artwork(item: HasLinks with HasRelativePath, out: OutputStream): Either[Exception, Unit] = {
    logger.info(s"Downloading artwork for ${item.relativePath}")
    data(item, _.artwork, out)
  }.error(s"Could not download the music for ${item.relativePath}")

  private def data(item: HasLinks, urlExtractor: Links => URL, out: OutputStream): Either[Exception, Unit] = {
    loadUrl(urlExtractor(item.links), out)
  }
}
