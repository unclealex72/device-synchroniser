package devsync.sync

import java.io.InputStream

import devsync.json.RelativePath

/**
  * Created by alex on 27/03/17
  **/
trait Resource {
  def find(path: RelativePath): Option[Resource]

  def copy(in: InputStream, relativePath: RelativePath, mimeType: String): Unit

  def open(relativePath: RelativePath): InputStream

  def remove(relativePath: RelativePath): Unit

  def canWrite: Boolean
}
