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
  * Created by alex on 12/04/17
  **/
abstract class ConstantProgressTask[T, V](task: jfxc.Task[V]) extends Task[V](task) {

  /**
    * A read only property that can change as this task progresses.
    * @return
    */
  def intermediateValue: ReadOnlyObjectProperty[Option[T]]

}

object ConstantProgressTask {

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

  def fromFuture[T, V](future: => EitherT[Future, Exception, V])
                      (implicit ec: ExecutionContext): ConstantProgressTask[T, V] = fromFuture(_ => future)
}

/**
  * A trait used to abstract away updating a task that can be referenced from a future.
  * @tparam T
  * @tparam V
  */
trait TaskUpdates[T, V] {
  def updateMessage(message: String): Unit
  def updateProgress(workDone: Long, max: Long): Unit
  def updateProgress(workDone: Double, max: Double): Unit
  def updateTitle(title: String): Unit
  def updateIntermediateValue(t: Option[T]): Unit
  def updateValue(v: V): Unit

  def updateMessageF(message: String)(implicit ec: ExecutionContext): EitherT[Future, Exception, Unit] =
    EitherT.right[Future, Exception, Unit](Future.successful(updateMessage(message)))
  def updateProgressF(workDone: Long, max: Long)(implicit ec: ExecutionContext): EitherT[Future, Exception, Unit] =
    EitherT.right[Future, Exception, Unit](Future.successful(updateProgress(workDone, max)))
  def updateProgressF(workDone: Double, max: Double)(implicit ec: ExecutionContext): EitherT[Future, Exception, Unit] =
    EitherT.right[Future, Exception, Unit](Future.successful(updateProgress(workDone, max)))
  def updateTitleF(title: String)(implicit ec: ExecutionContext): EitherT[Future, Exception, Unit] =
    EitherT.right[Future, Exception, Unit](Future.successful(updateTitle(title)))
  def updateIntermediateValueF(t: Option[T])(implicit ec: ExecutionContext): EitherT[Future, Exception, Unit] =
    EitherT.right[Future, Exception, Unit](Future.successful(updateIntermediateValue(t)))
  def updateValueF(v: V)(implicit ec: ExecutionContext): EitherT[Future, Exception, Unit] =
    EitherT.right[Future, Exception, Unit](Future.successful(updateValue(v)))
}