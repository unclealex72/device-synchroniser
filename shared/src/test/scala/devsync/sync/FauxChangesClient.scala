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

import java.io.{ByteArrayInputStream, FileNotFoundException, IOException, OutputStream}
import java.net.URL

import cats.syntax.either._
import devsync.json._
import devsync.remote.ChangesClient

/**
  * Created by alex on 06/04/17
  **/
case class FauxChangesClient(now: IsoDate, changes: FauxChange*) extends ChangesClient {

  var maybeSince: Option[IsoDate] = None

  val successUrl = new URL("http://localhost/success")
  val failureUrl = new URL("http://localhost/failure")

  val realChanges: Seq[Change] = {
    changes.map {
      case fa : FA => Addition(fa.relativePath, now, Links(successUrl, successUrl, successUrl))
      case ff : FF => Addition(ff.relativePath, now, Links(failureUrl, failureUrl, failureUrl))
      case fr : FR => Removal(fr.relativePath, now)
    }
  }

  def findChange(relativePath: RelativePath): Either[Exception, FA] = {
    def filterChange: FauxChange => Either[Exception, FA] = {
      case fa : FA => Right(fa)
      case _ : FF => Left(new IOException(s"Change $relativePath is marked as a failure"))
      case _ => Left(new FileNotFoundException(s"Change $relativePath is not an addition"))
    }
    for {
      change <- changes.find(fc => fc.relativePath == relativePath).toRight(
        new FileNotFoundException(s"Cannot find addition $relativePath"))
      filteredChange <- filterChange(change)
    } yield {
      filteredChange
    }
  }

  /**
    * Get the changes for a user since a specific date.
    */
  override def changesSince(user: String, maybeSince: Option[IsoDate]): Either[Exception, Changes] = Right {
    this.maybeSince = maybeSince
    Changes(realChanges)
  }

  /**
    * Count the number of changelog items for a user since a specific date
    */
  override def changelogSince(user: String, maybeSince: Option[IsoDate]): Either[Exception, Changelog] =
    Left(new Exception())

  override def music(item: HasLinks with HasRelativePath, out: OutputStream): Either[Exception, Unit] = {
    findChange(item.relativePath).flatMap { fa =>
      IO.closing(new ByteArrayInputStream(fa.content.getBytes("UTF-8"))) { in =>
        IO.copy(in, out)
      }
    }
  }

  override def tags(item: HasLinks with HasRelativePath): Either[Exception, Tags] =
    findChange(item.relativePath).flatMap { fa =>
      Right(Tags(
        albumArtistSort = fa.artist,
        albumArtist = fa.artist,
        album = fa.album,
        artist = fa.artist,
        artistSort = fa.artist,
        title = fa.title,
        totalDiscs = 1,
        totalTracks = 20,
        discNumber = 1,
        albumArtistId = fa.artist,
        albumId = fa.album,
        artistId = fa.artist,
        trackId = fa.title,
        asin = None,
        trackNumber = fa.track))
    }

  override def artwork(item: HasLinks with HasRelativePath, out: OutputStream): Either[Exception, Unit] =
    Left(new Exception())
}

sealed trait FauxChange {
  val artist: String
  val album: String
  val track: Int
  val title: String

  val relativePath = RelativePath(artist.toCharArray.headOption.map(_.toString).toSeq ++ Seq(artist, album, s"$track $title"))
}
case class FA(artist: String, album: String, track: Int, title: String, content: String) extends FauxChange
case class FR(artist: String, album: String, track: Int, title: String) extends FauxChange
case class FF(artist: String, album: String, track: Int, title: String, ex: Exception) extends FauxChange