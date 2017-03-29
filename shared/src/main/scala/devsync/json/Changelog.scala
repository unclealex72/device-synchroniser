package devsync.json

/**
  * Created by alex on 19/03/17
  **/
case class Changelog(total: Int, changelog: Seq[ChangelogItem])

case class ChangelogItem(parentRelativePath: RelativePath, at: IsoDate, relativePath: RelativePath, links: Links)

case class ChangelogCount(count: Int)