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
  * Helper methods dealing with I/O.
  */
object IO {

  /**
    * Copy an input stream to an output stream.
    * @param in The source input stream.
    * @param out The target output stream.
    */
  def copy(in: InputStream, out: OutputStream): Unit = {
    val bytes = new Array[Byte](16384) //16kb - Buffer size
    Iterator
      .continually(in.read(bytes))
      .takeWhile(_ != -1)
      .foreach(read=>out.write(bytes,0,read))
  }

  /**
    * Try running a block of code, making sure a closeable object is closed afterwards.
    * @param closeable The [[Closeable]] object to eventually close.
    * @param block The block of code to run.
    * @param afterClose The block of code to run after closing the closeable instance.
    * @tparam T The possible return type.
    * @tparam C The type of the closeable.
    * @return Either the result of running the block or an exception.
    */
  def closingTry[T, C <: Closeable](closeable: => C)
                                   (block: C => Try[T],
                                    afterClose: => Unit = {}): Try[T] = {
    try {
      block(closeable)
    }
    finally {
      Try(closeable.close())
      Try(afterClose)
    }
  }

  /**
    * Try running a block of code, making sure a closeable object is closed afterwards.
    * @param closeable The [[Closeable]] object to eventually close.
    * @param block The block of code to run.
    * @param afterClose The block of code to run after closing the closeable instance.
    * @tparam T The possible return type.
    * @tparam C The type of the closeable.
    * @return Either the result of running the block or an exception.
    */
  def closing[T, C <: Closeable](closeable: => C, afterClose: => Unit = {})(block: C => T): Try[T] = {
    closingTry(closeable)(cl => Try(block(cl)))
  }
}