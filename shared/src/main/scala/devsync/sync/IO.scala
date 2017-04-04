package devsync.sync

import java.io.{Closeable, InputStream, OutputStream}

import scala.util.Try

/**
  * Created by alex on 26/03/17
  **/
object IO {
  def copy(in: InputStream, out: OutputStream): Unit = {
    val bytes = new Array[Byte](16384) //16kb - Buffer size
    Iterator
      .continually(in.read(bytes))
      .takeWhile(_ != -1)
      .foreach(read=>out.write(bytes,0,read))
  }


  def closingTry[T, C <: Closeable](closeable: => C)(block: C => Either[Exception, T], afterClose: => Unit = {}): Either[Exception, T] = {
    try {
      block(closeable)
    }
    catch {
      case e: Exception => Left(e)
    }
    finally {
      Try(closeable.close())
      Try(afterClose)
    }
  }

  def closing[T, C <: Closeable](closeable: => C, afterClose: => Unit = {})(block: C => T): Either[Exception, T] = {
    closingTry(closeable)(cl => Right(block(cl)))
  }
}