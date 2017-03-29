package devsync.json

import java.net.URL

import cats.syntax.either._
import io.circe.{Decoder, DecodingFailure, Encoder, HCursor}
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
    Decoder.decodeString.emap(str => Either.fromTry(IsoDate(str)).leftMap(_.getMessage))
  implicit val encodeIsoDate: Encoder[IsoDate] = Encoder.encodeString.contramap(_.fmt)

  implicit val decodeLinks: Decoder[Links] =
    Decoder.forProduct3("music", "tags", "artwork")(Links.apply)

  implicit val decodeChangelogItem: Decoder[ChangelogItem] =
    Decoder.forProduct4("parentRelativePath", "at", "relativePath", "_links")(ChangelogItem.apply)

  implicit val decodeChangelog: Decoder[Changelog] =
    Decoder.forProduct2("total", "changelog")(Changelog.apply)

  implicit val decodeChangelogCount: Decoder[ChangelogCount] =
    Decoder.forProduct1("count")(ChangelogCount.apply)

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

  implicit val encodeDeviceDescriptor: Encoder[DeviceDescriptor] =
    Encoder.forProduct3("user", "lastModified", "offset")(dd => (dd.user, dd.maybeLastModified, dd.maybeOffset))

  override def parseChangelog(json: String): Try[Changelog] = decode[Changelog](json).toTry

  override def parseChangelogCount(json: String): Try[ChangelogCount] = decode[ChangelogCount](json).toTry

  override def parseChanges(json: String): Try[Changes] = decode[Changes](json).toTry

  override def parseTags(json: String): Try[Tags] = decode[Tags](json).toTry

  // Methods included for testing

  def parseChange(json: String): Try[Change] = decode[Change](json).toTry

  def parseChangelogItem(json: String): Try[ChangelogItem] = decode[ChangelogItem](json).toTry

  override def parseDeviceDescriptor(json: String): Try[DeviceDescriptor] = decode[DeviceDescriptor](json).toTry

  override def writeDeviceDescriptor(deviceDescriptor: DeviceDescriptor): String = deviceDescriptor.asJson.spaces2
}
