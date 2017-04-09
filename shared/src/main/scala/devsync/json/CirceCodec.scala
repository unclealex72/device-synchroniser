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
  * Created by alex on 20/03/17
  **/
class CirceCodec extends JsonCodec {

  implicit val decodeUrl: Decoder[URL] = Decoder.decodeString.emap(str => Right(new URL(str)))
  implicit val decodeRelativePath: Decoder[RelativePath] = Decoder.decodeString.emap(str => Right(RelativePath(str)))

  implicit val decodeIsoDate: Decoder[IsoDate] =
    Decoder.decodeString.emap(str => IsoDate(str).leftMap(_.getMessage))
  implicit val encodeIsoDate: Encoder[IsoDate] = Encoder.encodeString.contramap(_.fmt)

  implicit val decodeLinks: Decoder[Links] =
    Decoder.forProduct3("music", "tags", "artwork")(Links.apply)

  implicit val decodeChangelogItem: Decoder[ChangelogItem] =
    Decoder.forProduct4("parentRelativePath", "at", "relativePath", "_links")(ChangelogItem.apply)

  implicit val decodeChangelog: Decoder[Changelog] =
    Decoder.forProduct1("changelog")(Changelog.apply)

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
    Decoder.failedWithMessage[Change]("").
      or(predicatedDecoder("added", decodeAddition)).
      or(predicatedDecoder("removed", decodeRemoval))
  }

  implicit val decodeChanges: Decoder[Changes] = Decoder.forProduct1("changes")(Changes.apply)

  implicit val decodeTags: Decoder[Tags] = Decoder.forProduct15(
    "albumArtistSort", "albumArtist", "album", "artist", "artistSort", "title", "totalDiscs", "totalTracks",
    "discNumber", "albumArtistId", "albumId", "artistId", "trackId", "asin", "trackNumber")(Tags.apply)

  implicit val decodeDeviceDescriptor: Decoder[DeviceDescriptor] =
    Decoder.forProduct3("user", "lastModified", "offset")(DeviceDescriptor.apply)

  override def parseChangelog(json: String): Either[Exception, Changelog] = decode[Changelog](json)

  override def parseChanges(json: String): Either[Exception, Changes] = decode[Changes](json)

  override def parseTags(json: String): Either[Exception, Tags] = decode[Tags](json)

  // Methods included for testing

  def parseChange(json: String): Either[Exception, Change] = decode[Change](json)

  def parseChangelogItem(json: String): Either[Exception, ChangelogItem] = decode[ChangelogItem](json)

  override def parseDeviceDescriptor(json: String): Either[Exception, DeviceDescriptor] = decode[DeviceDescriptor](json)

  override def writeDeviceDescriptor(deviceDescriptor: DeviceDescriptor): String = {
    // Encoding fails in Android so just build the object explicitly.
    val map: Map[String, Json] =
      Map("user" -> Json.fromString(deviceDescriptor.user)) ++
        deviceDescriptor.maybeLastModified.map(isoDate => "lastModified" -> Json.fromString(isoDate.fmt)) ++
        deviceDescriptor.maybeOffset.map(offset => "offset" -> Json.fromInt(offset))
    Printer.noSpaces.copy(dropNullKeys = true).pretty(map.asJson)
  }
}
