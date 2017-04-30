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

package devsync.logging

import com.typesafe.scalalogging.StrictLogging

import scala.util.{Failure, Success, Try}

/**
  * Created by alex on 01/04/17
  * Syntactic sugar for logging in for comprehensions and Try statements
  **/
trait PassthroughLogging extends StrictLogging {

  implicit class AnyImplicits[V](v: V) {
    def info(message: => String): V = {
      logger.info(message)
      v
    }
  }

  implicit class TryImplicits[T](t: Either[Exception, T]) {
    def error(message: => String): Either[Exception, T] = {
      t match {
        case Left(e) => logger.error(message, e)
        case _ =>
      }
      t
    }
  }
}
