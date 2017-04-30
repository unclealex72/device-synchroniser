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

import cats.syntax.either._
import io.circe._
import io.circe.Decoder.Result
import io.circe.Json.{JObject, JString}
import io.circe.parser.decode
import io.circe.syntax._

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

  /**
    * A decoder that converts ISO8601 formatted strings to [[IsoDate]]
    */
  implicit val decodeIsoDate: Decoder[IsoDate] =
    Decoder.decodeString.emap(str => IsoDate(str).leftMap(_.getMessage))

  /**
    * An encoder that converts [[IsoDate]] to ISO8601 formatted strings.
    */
  implicit val encodeIsoDate: Encoder[IsoDate] = Encoder.encodeString.contramap(_.fmt)

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
  implicit val decodeDeviceDescriptor: Decoder[DeviceDescriptor] =
    Decoder.forProduct3("user", "lastModified", "offset")(DeviceDescriptor.apply)

  /**
    * @inheritdoc
    */
  override def parseChangelog(json: String): Either[Exception, Changelog] = decode[Changelog](json)

  /**
    * @inheritdoc
    */
  override def parseChanges(json: String): Either[Exception, Changes] = decode[Changes](json)

  /**
    * @inheritdoc
    */
  override def parseTags(json: String): Either[Exception, Tags] = decode[Tags](json)

  /**
    * Parse a [[Change]]. This method is visible for testing.
    * @param json The json to parse.
    * @return Either a [[Change]] or an exception.
    */
  def parseChange(json: String): Either[Exception, Change] = decode[Change](json)

  /**
    * Parse a [[ChangelogItem]]. This method is visible for testing.
    * @param json The json to parse.
    * @return Either a [[ChangelogItem]] or an exception.
    */
  def parseChangelogItem(json: String): Either[Exception, ChangelogItem] = decode[ChangelogItem](json)

  /**
    * @inheritdoc
    */
  override def parseDeviceDescriptor(json: String): Either[Exception, DeviceDescriptor] = decode[DeviceDescriptor](json)

  /**
    * @inheritdoc
    */
  override def writeDeviceDescriptor(deviceDescriptor: DeviceDescriptor): String = {
    // Encoding fails in Android so just build the object explicitly.
    val map: Map[String, Json] =
      Map("user" -> Json.fromString(deviceDescriptor.user)) ++
        deviceDescriptor.maybeLastModified.map(isoDate => "lastModified" -> Json.fromString(isoDate.fmt)) ++
        deviceDescriptor.maybeOffset.map(offset => "offset" -> Json.fromInt(offset))
    Printer.noSpaces.copy(dropNullKeys = true).pretty(map.asJson)
  }
}
