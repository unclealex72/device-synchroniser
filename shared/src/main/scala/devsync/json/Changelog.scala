package devsync.json

/**
  * Created by alex on 19/03/17
  **/
case class Changelog(items: Seq[ChangelogItem])

case class ChangelogItem(parentRelativePath: RelativePath,
                         at: IsoDate,
                         relativePath: RelativePath, links: Links) extends HasLinks with HasRelativePath
