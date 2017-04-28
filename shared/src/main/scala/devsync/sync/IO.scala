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