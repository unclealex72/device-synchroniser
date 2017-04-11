package devsync.scalafx

import javafx.{concurrent => jfxc}

import cats.data.EitherT
import cats.instances.future._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scalafx.concurrent.Task

/**
  * Created by alex on 09/04/17
  **/
object Tasks {

  def fromFuture[V](futureFactory: TaskUpdates[V] => EitherT[Future, Exception, V])(implicit ec: ExecutionContext): Task[V] = {
    new FutureScalaTask[V](futureFactory) {}
  }

  def fromFuture[V](future: => EitherT[Future, Exception, V])(implicit ec: ExecutionContext): Task[V] = {
    new FutureScalaTask[V](_ => future) {}
  }

  abstract class FutureScalaTask[V](futureFactory: TaskUpdates[V] => EitherT[Future, Exception, V])
                                   (implicit ec: ExecutionContext) extends Task[V](new jfxc.Task[V] {
    protected def call(): V = {
      implicit val _ec: ExecutionContext = ec
      val task = this
      val taskUpdates = new TaskUpdates[V] {
        override def updateMessage(message: String): Unit = task.updateMessage(message)
        override def updateProgress(workDone: Long, max: Long): Unit = task.updateProgress(workDone, max)
        override def updateProgress(workDone: Double, max: Double): Unit = task.updateProgress(workDone, max)
        override def updateTitle(title: String): Unit = task.updateTitle(title)
        override def updateValue(v: V): Unit = task.updateValue(v)
      }
      Await.result(futureFactory(taskUpdates).fold(e => throw e, v => v), Duration.Inf)
    }

  })
}

trait TaskUpdates[V] {

  def updateMessage(message: String): Unit
  def updateProgress(workDone: Long, max: Long): Unit
  def updateProgress(workDone: Double, max: Double): Unit
  def updateTitle(title: String): Unit
  def updateValue(v: V): Unit
}