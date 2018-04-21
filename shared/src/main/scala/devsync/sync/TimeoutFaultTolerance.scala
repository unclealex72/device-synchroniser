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

import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.Try

/**
  * The timeout pattern.
  */
class TimeoutFaultTolerance(
                           /**
                             * The amount of time to time out.
                             */
                           timeoutDuration: Duration)(implicit ec: ExecutionContext) extends FaultTolerance with StrictLogging {

  /**
    * Wrap code with fault tolerant patterns
    *
    * @param block The block of code to run.
    * @tparam R
    * @return The result of running the code.
    */
  override def tolerate[R](block: => Try[R]): Try[R] = Try {
    Await.result(Future { block.get }, timeoutDuration)
  }
}
