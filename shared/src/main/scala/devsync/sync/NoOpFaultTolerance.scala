/*
 * Copyright 2018 Alex Jones
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
import cats.data.EitherT

import scala.concurrent.{ExecutionContext, Future}

/**
  * A fault tolerance instance that does nothing.
  */
class NoOpFaultTolerance extends FaultTolerance {
  /**
    * Wrap code with fault tolerant patterns
    *
    * @param block The block of code to run.
    * @tparam R
    * @return The result of running the code.
    */
  override def tolerate[R](block: => EitherT[Future, Exception, R])(implicit ec: ExecutionContext): EitherT[Future, Exception, R] = {
    block
  }
}

/**
  * A single instance of the no-op fault tolerance.
  */
object NoOpFaultTolerance extends NoOpFaultTolerance