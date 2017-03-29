package devsync.json

import java.net.URL

import org.specs2.mutable.Specification

import scala.io.Source
import scala.util.{Success, Try}
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
          at = "2017-03-13T22:05:16.000Z",
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
          at = "2017-03-22T17:18:55.000Z"
        )
      )
    }
  }

  "Reading a list of changes" should {
    "be able to read how many changes occurred" in {
      "changes.json".deserialiseUsing(_.parseChanges).changes.size must be_==(19)
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

  "Reading a changelog count" should {
    "correctly read the number of changelog items" in {
      "changelog-count.json".deserialiseUsing(_.parseChangelogCount) must be_==(ChangelogCount(2))
    }
  }

  "Reading a full changelog count" should {
    "correctly deserialise" in {
      "changelog.json".deserialiseUsing(_.parseChangelog) must be_==(
        Changelog(
          total = 2,
          changelog = Seq(
            ChangelogItem(
              parentRelativePath = "Q/Queen/Flash Gordon",
              at = "2017-03-13T22:05:06.000Z",
              relativePath = "Q/Queen/Flash Gordon/01 Flashs Theme.mp3",
              links = Links(
                music = "http://localhost:9000/music/alex/Q/Queen/Flash+Gordon/01+Flashs+Theme.mp3",
                tags = "http://localhost:9000/tags/alex/Q/Queen/Flash+Gordon/01+Flashs+Theme.mp3",
                artwork = "http://localhost:9000/artwork/alex/Q/Queen/Flash+Gordon/01+Flashs+Theme.mp3"
              )
            ),
            ChangelogItem(
              parentRelativePath = "N/Napalm Death/Scum",
              at = "2017-03-13T22:05:01.000Z",
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
  implicit def stringToIsoDate(str: String): IsoDate = IsoDate(str).get
}
