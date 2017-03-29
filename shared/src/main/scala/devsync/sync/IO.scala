package devsync.sync

import java.io.{Closeable, InputStream, OutputStream}

import scala.util.Try

/**
  * Created by alex on 26/03/17
  **/
object IO {
  def copy(in: InputStream, out: OutputStream): Unit = {
    val bytes = new Array[Byte](8192) //1024 bytes - Buffer size
    Iterator
      .continually(in.read(bytes))
      .takeWhile(_ != -1)
      .foreach(read=>out.write(bytes,0,read))
  }


  def closing[T, C <: Closeable](closeable: C)(block: C => T): T = {
    try {
      block(closeable)
    }
    finally {
      Try(closeable.close())
    }
  }

  def closing2[T, C1 <: Closeable, C2 <: Closeable](closeable1: C1, closeable2: C2)(block: (C1, C2) => T): T = {
    try {
      block(closeable1, closeable2)
    }
    finally {
      Seq(closeable1, closeable2).foreach(c => Try(c.close()))
    }
  }
}