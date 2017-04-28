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

import devsync.common.PassthroughLogging
import devsync.json.IsoDate._
import devsync.json.RelativePath._
import devsync.json.{RelativePath, _}
import devsync.sync.IO

import cats.syntax.either._


/**
  * Created by alex on 24/03/17
  **/
class ChangesClientImpl(val jsonCodec: JsonCodec, val baseUrl: URL) extends ChangesClient with PassthroughLogging {
  /**
    * Get the changes for a user since a specific date.
    */
  override def changesSince(user: String, maybeSince: Option[IsoDate]): Either[Exception, Changes] = {
    val since = orDatum(maybeSince)
    logger.info(s"Looking for changes since $since")
    readUrl(_.parseChanges, "changes" / user / since).error(s"Could not download changes since $since")
  }

  /**
    * Count the number of changelog items for a user since a specific date
    */
  override def changelogSince(user: String, maybeSince: Option[IsoDate]): Either[Exception, Changelog] = {
    val since = orDatum(maybeSince)
    logger.info(s"Loading the changelog since $since")
    readUrl(_.parseChangelog, "changelog" / user / since).error(s"Could not download the changelog since $since")
  }

  def orDatum(maybeSince: Option[IsoDate]): IsoDate = maybeSince.getOrElse(IsoDate(new Date(0)))

  def readUrl[T](parser: JsonCodec => String => Either[Exception, T], relativePath: RelativePath): Either[Exception, T] = {
    val url = baseUrl / relativePath
    for {
      body <- readUrlAsString(url)
      result <- parser(jsonCodec)(body)
    } yield result
  }


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

  def readUrlAsString(url: URL): Either[Exception, String] = {
    val buff = new ByteArrayOutputStream
    loadUrl(url, buff).map(_ => buff.toString("UTF-8"))
  }.error(s"Could not read the data from")

  override def tags(item: HasLinks with HasRelativePath): Either[Exception, Tags] = {
    for {
      tagsData <- readUrlAsString(item.links.tags)
      tags <- jsonCodec.parseTags(tagsData)
    } yield tags
  }

  override def music(item: HasLinks with HasRelativePath, out: OutputStream): Either[Exception, Unit] = {
    logger.info(s"Downloading music for ${item.relativePath}")
    data(item, _.music, out)
  }.error(s"Could not download the music for ${item.relativePath}")

  override def artwork(item: HasLinks with HasRelativePath, out: OutputStream): Either[Exception, Unit] = {
    logger.info(s"Downloading artwork for ${item.relativePath}")
    data(item, _.artwork, out)
  }.error(s"Could not download the music for ${item.relativePath}")

  def data(item: HasLinks, urlExtractor: Links => URL, out: OutputStream): Either[Exception, Unit] = {
    loadUrl(urlExtractor(item.links), out)
  }
}
