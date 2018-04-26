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

package devsync.json

import java.net.URL

import devsync.json.Extension.{M4A, MP3}
import org.specs2.mutable.Specification
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.{Instant, ZoneId}

import scala.io.Source
import scala.util.Try

/**
  * Created by alex on 20/03/17
  **/
class CirceCodecSpec extends Specification {

  val codec = new CirceCodec

  "Reading an addition" should {
    "correctly deserialise" in {
      "added.json".deserialiseUsing(_.parseChange) must be_==(
        Addition(
          relativePath = "Q/Queen/Flash Gordon/01 Flashs Theme.mp3",
          at = "2017-03-13T22:05:16Z",
          links = Links(
            music = "http://localhost:9000/music/alex/Q/Queen/Flash Gordon/01 Flashs Theme.mp3",
            tags = "http://localhost:9000/tags/alex/Q/Queen/Flash Gordon/01 Flashs Theme.mp3",
            artwork = "http://localhost:9000/tags/alex/Q/Queen/Flash Gordon/01 Flashs Theme.mp3"
          )
        )
      )
    }
  }

  "Reading a removal" should {
    "correctly deserialise" in {
      "removed.json".deserialiseUsing(_.parseChange) must be_==(
        Removal(
          relativePath = "N/Napalm Death/Scum/12 You Suffer.mp3",
          at = "2017-03-22T17:18:55Z"
        )
      )
    }
  }

  "Reading a list of changes" should {
    "be able to read how many changes occurred" in {
      "changes.json".deserialiseUsing(_.parseChanges).changes.size must be_==(29)
    }
  }

  "Reading tags" should {
    "correctly deserialise" in {
      "tags.json".deserialiseUsing(_.parseTags) must be_==(
        Tags(
          albumArtistSort = "Queen",
          albumArtist = "Queen",
          album = "Flash Gordon",
          artist = "Queen",
          artistSort = "Queen",
          title = "In the Space Capsule (The Love Theme)",
          totalDiscs = 1,
          totalTracks = 18,
          discNumber = 1,
          albumArtistId = "0383dadf-2a4e-4d10-a46a-e9e041da8eb3",
          albumId = "c8a6c0d4-1fe4-4940-a0df-cfb112f9800c",
          artistId = "0383dadf-2a4e-4d10-a46a-e9e041da8eb3",
          trackId = "7290763c-c6ce-48a1-b603-39f11e018239",
          asin = Some("B004Z5450C"),
          trackNumber = 2
        )
      )
    }
  }

  "Reading a full changelog count" should {
    "correctly deserialise" in {
      "changelog.json".deserialiseUsing(_.parseChangelog) must be_==(
        Changelog(Seq(
            ChangelogItem(
              parentRelativePath = "Q/Queen/Flash Gordon",
              at = "2017-03-13T22:05:06Z",
              relativePath = "Q/Queen/Flash Gordon/01 Flashs Theme.mp3",
              links = Links(
                music = "http://localhost:9000/music/alex/Q/Queen/Flash+Gordon/01+Flashs+Theme.mp3",
                tags = "http://localhost:9000/tags/alex/Q/Queen/Flash+Gordon/01+Flashs+Theme.mp3",
                artwork = "http://localhost:9000/artwork/alex/Q/Queen/Flash+Gordon/01+Flashs+Theme.mp3"
              )
            ),
            ChangelogItem(
              parentRelativePath = "N/Napalm Death/Scum",
              at = "2017-03-13T22:05:01Z",
              relativePath = "N/Napalm Death/Scum/12 You Suffer.mp3",
              links = Links(
                music = "http://localhost:9000/music/alex/N/Napalm+Death/Scum/12+You+Suffer.mp3",
                tags = "http://localhost:9000/tags/alex/N/Napalm+Death/Scum/12+You+Suffer.mp3",
                artwork = "http://localhost:9000/artwork/alex/N/Napalm+Death/Scum/12+You+Suffer.mp3"
              )
            )
          )
        )
      )
    }

    "Writing a device descriptor with just a user and extension" should {
      "product a json object with just the user field" in {
        codec.writeDeviceDescriptor(DeviceDescriptor("alex", M4A, None, None)) must be_==("""{"user":"alex","extension":"m4a"}""")
      }
    }

    "Writing a device descriptor with all fields" should {
      "product a json object with all fields populated" in {
        codec.writeDeviceDescriptor(
          DeviceDescriptor("alex", MP3, Some("2017-03-13T22:05:01Z"), Some(5))) must be_==(
          """{"user":"alex","extension":"mp3","lastModified":"2017-03-13T22:05:01Z","offset":5}""")
      }
    }
  }

  implicit class StringImplicits(str: String) {
    def deserialiseUsing[V](method: CirceCodec => (String => Try[V])): V = {
      val data = Source.fromInputStream(getClass.getResourceAsStream(str)).mkString
      method(new CirceCodec)(data).recover {
        case e: Exception => throw e
      }.get
    }

  }

  implicit def stringToUrl(str: String): URL = new URL(str)
  implicit def stringToRelativePath(str: String): RelativePath = RelativePath(str)
  implicit def stringToIsoDate(str: String): Instant = {
    DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.systemDefault()).parse(str, Instant.FROM)
  }
}
