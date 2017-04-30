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

package devsync.scalafx.util

import javafx.{concurrent => jfxc}

import cats.data.EitherT

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scalafx.application.Platform
import scalafx.beans.property.{ObjectProperty, ReadOnlyObjectProperty}
import scalafx.concurrent.Task
import cats.instances.future._

/**
  * A task that can emit intermediate values as it progresses. The type of the intermediate value does not
  * have to be the same as the final value of the task.
  * @param task An underlying JavaFX task.
  * @tparam T The type of intermediate values.
  * @tparam V The type of the final value.
  **/
abstract class ConstantProgressTask[T, V](task: jfxc.Task[V]) extends Task[V](task) {

  /**
    * A read only property that can change as this task progresses.
    * @return A read only property that can change as this task progresses.
    */
  def intermediateValue: ReadOnlyObjectProperty[Option[T]]

}

/**
  * Used to create instances of [[ConstantProgressTask]].
  */
object ConstantProgressTask {

  /**
    * Create a [[ConstantProgressTask]] from a future.
    * @param futureFactory A function that takes a [[TaskUpdates]] and returns a future.
    * @param ec The execution context used to run the task in the background.
    * @tparam T The type of intermediate values.
    * @tparam V The type of the final value.
    * @return A new constant progress task based on the supplied future.
    */
  def fromFuture[T, V](futureFactory: TaskUpdates[T, V] => EitherT[Future, Exception, V])
                      (implicit ec: ExecutionContext): ConstantProgressTask[T, V] = {
    val _intermediateValue: ObjectProperty[Option[T]] = ObjectProperty(this, "intermediateValue", None)
    def _updateIntermediateValue(maybeValue: Option[T]): Unit = {
      Platform.runLater {
        _intermediateValue.value = maybeValue
      }
    }
    new ConstantProgressTask[T, V](new jfxc.Task[V] {
      protected def call(): V = {
        val task = this
        val taskUpdates = new TaskUpdates[T, V] {
          override def updateMessage(message: String): Unit = task.updateMessage(message)
          override def updateProgress(workDone: Long, max: Long): Unit = task.updateProgress(workDone, max)
          override def updateProgress(workDone: Double, max: Double): Unit = task.updateProgress(workDone, max)
          override def updateTitle(title: String): Unit = task.updateTitle(title)
          override def updateIntermediateValue(t: Option[T]): Unit = _updateIntermediateValue(t)
          override def updateValue(v: V): Unit = task.updateValue(v)
        }
        Await.result(futureFactory(taskUpdates).fold(e => throw e, v => v), Duration.Inf)
      }
    }) {
      override def intermediateValue: ReadOnlyObjectProperty[Option[T]] = _intermediateValue
    }
  }

  /**
    * Create a [[ConstantProgressTask]] from a future that does not worry about feeding back updates.
    * @param future The code to be run in the task.
    * @param ec The execution context used to run the task in the background.
    * @tparam T The type of intermediate values.
    * @tparam V The type of the final value.
    * @return A new constant progress task based on the supplied future.
    */
  def fromFuture[T, V](future: => EitherT[Future, Exception, V])
                      (implicit ec: ExecutionContext): ConstantProgressTask[T, V] = fromFuture(_ => future)
}

/**
  * A trait used to abstract away updating a task that can be referenced from a future.
  * @tparam T The type of intermediate values.
  * @tparam V The type of the final value.
  */
trait TaskUpdates[T, V] {

  /**
    * Update the task's message.
    * @param message The new value of the task's message.
    */
  def updateMessage(message: String): Unit

  /**
    * Update the task's progress.
    * @param workDone The amount of work done.
    * @param max The maximum amount of work.
    */
  def updateProgress(workDone: Long, max: Long): Unit

  /**
    * Update the task's progress.
    * @param workDone The amount of work done.
    * @param max The maximum amount of work.
    */
  def updateProgress(workDone: Double, max: Double): Unit

  /**
    * Update the task's title.
    * @param title The new value of the task's title.
    */
  def updateTitle(title: String): Unit

  /**
    * Update the task's intermediate value.
    * @param t The new value of the task's intermediate value.
    */
  def updateIntermediateValue(t: Option[T]): Unit

  /**
    * Update the task's final value.
    * @param v The new value of the task's final value.
    */
  def updateValue(v: V): Unit

  /**
    * Eventually update the task's message.
    * @param message The new value of the task's message.
    * @param ec An execution context used to run the update command.
    * @return Eventually either [[Unit]] or an exception.
    */
  def updateMessageF(message: String)(implicit ec: ExecutionContext): EitherT[Future, Exception, Unit] =
    EitherT.right[Future, Exception, Unit](Future.successful(updateMessage(message)))

  /**
    * Eventually update the task's progress.
    * @param workDone The amount of work done.
    * @param max The maximum amount of work.
    * @param ec An execution context used to run the update command.
    * @return Eventually either [[Unit]] or an exception.
    */
  def updateProgressF(workDone: Long, max: Long)(implicit ec: ExecutionContext): EitherT[Future, Exception, Unit] =
    EitherT.right[Future, Exception, Unit](Future.successful(updateProgress(workDone, max)))

  /**
    * Eventually update the task's progress.
    * @param workDone The amount of work done.
    * @param max The maximum amount of work.
    * @param ec An execution context used to run the update command.
    * @return Eventually either [[Unit]] or an exception.
    */
  def updateProgressF(workDone: Double, max: Double)(implicit ec: ExecutionContext): EitherT[Future, Exception, Unit] =
    EitherT.right[Future, Exception, Unit](Future.successful(updateProgress(workDone, max)))

  /**
    * Eventually update the task's title.
    * @param title The new value of the task's title.
    * @param ec An execution context used to run the update command.
    * @return Eventually either [[Unit]] or an exception.
    */
  def updateTitleF(title: String)(implicit ec: ExecutionContext): EitherT[Future, Exception, Unit] =
    EitherT.right[Future, Exception, Unit](Future.successful(updateTitle(title)))

  /**
    * Eventually update the task's intermediate value.
    * @param t The new value of the task's intermediate value.
    * @param ec An execution context used to run the update command.
    * @return Eventually either [[Unit]] or an exception.
    */
  def updateIntermediateValueF(t: Option[T])(implicit ec: ExecutionContext): EitherT[Future, Exception, Unit] =
    EitherT.right[Future, Exception, Unit](Future.successful(updateIntermediateValue(t)))

  /**
    * Eventually update the task's final value.
    * @param v The new value of the task's final value.
    * @param ec An execution context used to run the update command.
    * @return Eventually either [[Unit]] or an exception.
    */
  def updateValueF(v: V)(implicit ec: ExecutionContext): EitherT[Future, Exception, Unit] =
    EitherT.right[Future, Exception, Unit](Future.successful(updateValue(v)))
}