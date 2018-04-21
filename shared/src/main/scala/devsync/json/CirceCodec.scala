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

import cats.syntax._
import cats.implicits._
import io.circe._
import io.circe.Decoder.Result
import io.circe.Json.{JObject, JString}
import io.circe.parser.decode
import io.circe.syntax._
import org.threeten.bp.{Instant, ZoneId}
import org.threeten.bp.format.DateTimeFormatter

import scala.util.Try

/**
  * An implementation of [[JsonCodec]] that uses [[https://circe.github.io/circe/ Circe]]
  **/
class CirceCodec extends JsonCodec {

  /**
    * A decoder that converts strings to URLs.
    */
  implicit val decodeUrl: Decoder[URL] = Decoder.decodeString.emap(str => Right(new URL(str)))

  /**
    * A decoder that converts strings to [[RelativePath]]s
    */
  implicit val decodeRelativePath: Decoder[RelativePath] = Decoder.decodeString.emap(str => Right(RelativePath(str)))

  private val isoFormatter: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.systemDefault())

  /**
    * A decoder that converts ISO8601 formatted strings to an [[Instant]]
    */
  implicit val decodeInstant: Decoder[Instant] =
    Decoder.decodeString.emap { str =>
      try {
        Right(isoFormatter.parse(str, Instant.FROM))
      }
      catch {
        case t: Throwable => Left(t.getMessage)
      }
    }

  implicit val decodeExtension: Decoder[Extension] = Decoder.decodeString.emap { str =>
    Extension.values.find(_.extension == str).toRight(s"$str is not a valid extension.")
  }

  /**
    * An encoder that converts [[Instant]] to ISO8601 formatted strings.
    */
  implicit val encodeIsoDate: Encoder[Instant] = Encoder.encodeString.contramap(i => isoFormatter.format(i))

  /**
    * A decoder [[Links]].
    */
  implicit val decodeLinks: Decoder[Links] =
    Decoder.forProduct3("music", "tags", "artwork")(Links.apply)

  /**
    * A decoder for [[ChangelogItem]]s.
    */
  implicit val decodeChangelogItem: Decoder[ChangelogItem] =
    Decoder.forProduct4("parentRelativePath", "at", "relativePath", "_links")(ChangelogItem.apply)

  /**
    * A decoder for [[Changelog]]s.
    */
  implicit val decodeChangelog: Decoder[Changelog] =
    Decoder.forProduct1("changelog")(Changelog.apply)

  /**
    * A decoder for [[Addition]]s and [[Removal]]s that adds an action field that describes what class to
    * decode to.
    */
  implicit val decodeChange: Decoder[Change] = {
    val decodeAddition: Decoder[Change] = Decoder.forProduct3("relativePath", "at", "_links")(Addition.apply)
    val decodeRemoval: Decoder[Change] = Decoder.forProduct2("relativePath", "at")(Removal.apply)
    def predicatedDecoder(action: String, decoder: Decoder[Change]): Decoder[Change] =
      decoder.validate({ c =>
        (for {
          obj <- c.value.asObject
          actionField <- obj("action")
          fieldValue <- actionField.asString if fieldValue == action
        } yield {}).isDefined}, action)
    Decoder.failedWithMessage[Change]("Could not find a valid action type").
      or(predicatedDecoder("removed", decodeRemoval)).
      or(predicatedDecoder("added", decodeAddition))
  }

  /**
    * A decoder for [[Changes]].
    */
  implicit val decodeChanges: Decoder[Changes] = Decoder.forProduct1("changes")(Changes.apply)

  /**
    * A decoder for [[Tags]].
    */
  implicit val decodeTags: Decoder[Tags] = Decoder.forProduct15(
    "albumArtistSort", "albumArtist", "album", "artist", "artistSort", "title", "totalDiscs", "totalTracks",
    "discNumber", "albumArtistId", "albumId", "artistId", "trackId", "asin", "trackNumber")(Tags.apply)

  /**
    * A decoder for [[DeviceDescriptor]]s.
    */
  implicit val decodeDeviceDescriptor: Decoder[DeviceDescriptor] = {
    Decoder.forProduct4("user", "extension", "lastModified", "offset")(DeviceDescriptor.optionalExtension)
  }

  /**
    * @inheritdoc
    */
  override def parseChangelog(json: String): Try[Changelog] = parse[Changelog](json)

  /**
    * @inheritdoc
    */
  override def parseChanges(json: String): Try[Changes] = parse[Changes](json)

  /**
    * @inheritdoc
    */
  override def parseTags(json: String): Try[Tags] = parse[Tags](json)

  /**
    * Parse a [[Change]]. This method is visible for testing.
    * @param json The json to parse.
    * @return Either a [[Change]] or an exception.
    */
  def parseChange(json: String): Try[Change] = parse[Change](json)

  /**
    * Parse a [[ChangelogItem]]. This method is visible for testing.
    * @param json The json to parse.
    * @return Either a [[ChangelogItem]] or an exception.
    */
  def parseChangelogItem(json: String): Try[ChangelogItem] = parse[ChangelogItem](json)

  /**
    * @inheritdoc
    */
  override def parseDeviceDescriptor(json: String): Try[DeviceDescriptor] = parse[DeviceDescriptor](json)

  private def parse[A](json: String)(implicit ev: Decoder[A]): Try[A] = {
    decode[A](json).toTry
  }

  /**
    * @inheritdoc
    */
  override def writeDeviceDescriptor(deviceDescriptor: DeviceDescriptor): String = {
    // Encoding fails in Android so just build the object explicitly.
    val map: Map[String, Json] =
      Map("user" -> Json.fromString(deviceDescriptor.user), "extension" -> Json.fromString(deviceDescriptor.extension.extension)) ++
        deviceDescriptor.maybeLastModified.map(instant => "lastModified" -> Json.fromString(isoFormatter.format(instant))) ++
        deviceDescriptor.maybeOffset.map(offset => "offset" -> Json.fromInt(offset))
    Printer.noSpaces.copy(dropNullKeys = true).pretty(map.asJson)
  }
}
